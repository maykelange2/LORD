package com.kleegroup.lord.moteur;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import com.kleegroup.lord.moteur.Fichier.ETAT;
import com.kleegroup.lord.moteur.exceptions.EchecCreationLogs;
import com.kleegroup.lord.moteur.logs.ILogger;
import com.kleegroup.lord.moteur.logs.LoggueurFichierCSV;
import com.kleegroup.lord.moteur.logs.LoggueurMultiple;
import com.kleegroup.lord.moteur.logs.LoggueurRam;
import com.kleegroup.lord.moteur.reader.CsvReaderAdapter;
import com.kleegroup.lord.moteur.util.FichierComparteurGroupe;
import com.kleegroup.lord.moteur.util.FichierComparteurOrdreTopo;
import com.kleegroup.lord.moteur.util.ICSVDataSource;
import com.kleegroup.lord.moteur.util.INotifiable;
import com.kleegroup.lord.moteur.util.LogFilesZipper;
import com.kleegroup.lord.moteur.util.SeparateurChamps;
import com.kleegroup.lord.moteur.util.SeparateurDecimales;


/**
 * Sert a ordonner les fichier selon leur dependeances.<br>
 * voir http://en.wikipedia.org/wiki/Topological_sorting
 */
public class Schema implements INotifiable {

	private static org.apache.log4j.Logger logAppli = Logger.getLogger(Schema.class);

	protected List<Fichier> fichiers = new ArrayList<>();

	protected int niveauActuel = -1;

	protected Map<String, ILogger> loggeurs = new HashMap<>();

	protected INotifiable eltANotifier = null;

	protected String encoding;

	protected String separateurLignes;

	protected String charEchapGuillemets;

	protected boolean ignorerGuillemets;

	protected boolean afficherExportLogs;

	protected Categories categories = new Categories();

	protected SeparateurChamps separateurChamp = SeparateurChamps.SEPARATEUR_POINT_VIRGULE;

	protected SeparateurDecimales separateurDecimales = SeparateurDecimales.SEPARATEUR_VIRGULE;

	List<String> listCheminFichiersLog = new ArrayList<>();

	private Fichier fichierEnCours;

	private String emplacementFichiersLogs = "./logs/";

	private final SortedMap<Integer, HashSet<Fichier>> groupes = new TreeMap<>();

	private File fichierLogGeneral;
	private PrintStream loggueurGeneral = null;
	private Date dateDebut = null;

	/**
	 * Ajoute le fichier au schema, en fin de la liste des fichiers.
	 * 
	 * @param f	fichier a rajouter au schema
	 */
	public void addFichier(Fichier f) {
		addFichier(f, fichiers.size());
	}

	/**
	 * Ajoute le fichier f au schema à la position pos.
	 * 
	 * @param f
	 *            fichier a rajouter au schema
	 * @param pos
	 *            la position du fichier
	 */
	public void addFichier(Fichier f, int pos) {
		fichiers.add(pos, f);
		categories.put(f, f.getNomCategorie());
	}

	/**
	 * Met en pause la vérification du fichier actuel. La vérification peut continuer
	 * en appelant {@link #resume()}
	 */
	public void pause() {
		fichierEnCours.pause();
	}

	/**
	 * Lance la vérification du schéma.
	 */
	public void verifie() {
		logAppli.info("debut de la verification du schema");

		clean();

		for (final Fichier f : fichiers) {

			if (f.getGroupe() <= niveauActuel && f.isPretVerif()) {

				creerSourceDonnee(f);// ouvre le fichier CSV en lecture
				fichierEnCours = f;
				f.verifie();
				marquerFichierVerifie(f);
				System.out.println(f.getNom() + " " + f.getEtatFichier());

				if (f.getEtatFichier() == ETAT.ABANDONNE_UTILISATEUR) {
					for (final Fichier f2 : fichiers) {
						if (f2.isEnabled()) {
							f2.setEtatFichier(ETAT.ABANDONNE_UTILISATEUR);
							finFichier(f2.getNom(), f2.getEtatFichier());
						}
					}
					return;
				}
			} else {
				// signaler l'echec de la verification a cause d'un
				// fichier dependant non verifie
				if (f.getEtatFichier() == ETAT.EN_ATTENTE) {
					f.abandonFichier(ETAT.ABANDONNE_DEPENDANCE);
				}

			}
		}
		creerLogGeneral();

		logAppli.info("fin de la verification du schema");
	}

	private void creerSourceDonnee(Fichier f) {
		final ICSVDataSource source = new CsvReaderAdapter(f.getChemin(), encoding);
		source.setFieldSeparator(separateurChamp.value());
		f.setSource(source);
	}

	/**
	 * Nettoie l'objet. Remet à zero les données spécifiques utilisées lors de la
	 * dernière vérification pour pouvoir réutiliser cet objet pour une nouvelle
	 * vérification
	 */
	public void clean() {
		niveauActuel = 0;

		for (final Fichier f : fichiers) {
			f.clean();
		}
		trierLesFichiers();
		calculGroupesDisponibles();
		try {
			creerDossierEtFichiersLogs();

			initialiserLesFichiers();

		} catch (final Exception e) {
			logAppli.error(e);
		}
	}

	private void marquerFichierVerifie(Fichier f) {
		/*
		 * supprimer le fichier de son groupe.cela permet de determiner quand
		 * tous les fichiers d'un groupe ont ete verifie et de passer au groupe
		 * suivant
		 */

		groupes.get(f.getGroupe()).remove(f);
		if (groupes.get(f.getGroupe()).isEmpty()) {

			groupes.remove(f.getGroupe());
			try {
				if (!groupes.isEmpty()) {
					niveauActuel = groupes.firstKey();
				}
			} catch (final NoSuchElementException ex) {
				logAppli.error(ex);
			}
		}
	}

	private void creerDossierEtFichiersLogs() throws EchecCreationLogs {
		final File dirLog = new File(emplacementFichiersLogs);
		if (!dirLog.exists()) {
			if (!dirLog.mkdir()) {
				throw new EchecCreationLogs();
			}
		}

	}

	private void initialiserLesFichiers() {
		listCheminFichiersLog.clear();
		dateDebut = Calendar.getInstance().getTime();
		for (final Fichier f : fichiers) {
			final LoggueurMultiple lm = new LoggueurMultiple();
			lm.setNomFichier(f.getNom());
			final String timestamp = (new SimpleDateFormat("yyyyMMddHHmmss")).format(dateDebut) + "-";

			final String chemin = emplacementFichiersLogs + timestamp + f.getNom() + ".csv";
			listCheminFichiersLog.add(chemin);
			final ILogger logCSV = new LoggueurFichierCSV(chemin);
			logCSV.setNomFichier(f.getNom());

			final ILogger logRam = new LoggueurRam();
			logRam.setNomFichier(f.getNom());

			lm.addLogger(logRam);
			lm.addLogger(logCSV);

			f.setLogger(lm);
			loggeurs.put(f.getNom(), logRam);

			if (f.getEtatFichier() != Fichier.ETAT.DESACTIVE_DEPENDANCE && f.getEtatFichier() != Fichier.ETAT.DESACTIVE_UTILISATEUR) {
				f.setEtatFichier(Fichier.ETAT.EN_ATTENTE);
			}
		}
	}

	private void creerLogGeneral() {
		final String timestamp = (new SimpleDateFormat("yyyyMMddHHmmss")).format(dateDebut) + "-";
		try {
			fichierLogGeneral = File.createTempFile(timestamp + " - LogGeneral ", ".log");
			try (FileOutputStream fos = new FileOutputStream(fichierLogGeneral)) {
				loggueurGeneral = new PrintStream(new FileOutputStream(fichierLogGeneral));
				construireLogGeneral(loggueurGeneral);
				listCheminFichiersLog.add(fichierLogGeneral.getAbsolutePath());

			} catch (final FileNotFoundException e) {
				logAppli.error(e);
			}
		} catch (final IOException e) {
			fichierLogGeneral = null;
			logAppli.error(e);
		}

	}

	private void construireLogGeneral(PrintStream log) {
		log.println("date de debut de  verification" + dateDebut);
		for (final Fichier f : fichiers) {
			log.println(f.getNom() + " : ETAT FINAL : " + f.getEtatFichier());
			log.println(f.getNom() + " : CHEMIN : " + f.getChemin());
		}

	}

	private void calculGroupesDisponibles() {
		groupes.clear();
		for (final Fichier f : fichiers) {
			if (f.isEnabled()) {
				if (groupes.get(f.getGroupe()) == null) {
					groupes.put(f.getGroupe(), new HashSet<Fichier>());
				}
				groupes.get(f.getGroupe()).add(f);
			}
		}
	}

	protected void trierLesFichiers() {

		// trier par ordre de dependance (ordre "topologique")
		/* voir Fichier#getNiveauTopo */
		Collections.sort(fichiers, new FichierComparteurOrdreTopo());

		// trier les fichiers par ordre de groupe(défini dans les specs)
		Collections.sort(fichiers, new FichierComparteurGroupe());
		niveauActuel = fichiers.get(0).getGroupe();
	}

	/**
	 * @return la liste des fichier du schema.
	 */
	public List<Fichier> getFichiers() {
		return fichiers;
	}


	/**
	 * renvoie le fichier identifie par ce nom.
	 * 
	 * @param nom
	 *            le nom du fichier
	 * @return le fichier correspondant
	 */
	public Fichier getFichier(String nom) {
		for (final Fichier f : fichiers) {
			if (nom.equals(f.getNom())) {
				return f;
			}
		}
		return null;
	}

	/**
	 * @return une map contenant les logueurs(la liste des erreurs) de chaque
	 *         fichier
	 */
	public Map<String, ILogger> getLoggeurs() {
		return loggeurs;
	}

	/** {@inheritDoc} */
	@Override
	public void finFichier(String nomFichier, ETAT etat) {
		if (eltANotifier != null) {
			eltANotifier.finFichier(nomFichier, etat);
		}

	}

	/** {@inheritDoc} */
	@Override
	public void caractereTraites(long nbCaracteres) {
		if (eltANotifier != null) {
			eltANotifier.caractereTraites(nbCaracteres);
		}
	}

	/** {@inheritDoc} */
	@Override
	public void debutFichier(String nomFichier) {
		if (eltANotifier != null) {
			eltANotifier.debutFichier(nomFichier);
		}
	}

	/**
	 * désigne l'objet à notifier lorsque certains évenements (voir
	 * {@link INotifiable}) ont lieu.
	 * 
	 * @param eltANotifier
	 *            l'objet à notifier
	 */
	public void setEltANotifier(INotifiable eltANotifier) {
		this.eltANotifier = eltANotifier;
		for (final Fichier f : fichiers) {
			f.setEltAnotifier(eltANotifier);
		}
	}

	/**
	 * Reprend la vérification si elle est en pause. voir {@link #pause()}.
	 */
	public void resume() {
		fichierEnCours.resume();
	}

	/**
	 * Annule la vérification du fichier en cours.
	 */
	public void cancel() {
		fichierEnCours.cancel();

	}

	/**
	 * @return le repertoire où seront sauvegardés les fichiers de logs.
	 */
	public String getEmplacementFichiersLogs() {
		return emplacementFichiersLogs;
	}

	/**
	 * @param emplacementFichiersLogs
	 *            repertoire où seront sauvegardés les fichiers de logs.
	 */
	public void setEmplacementFichiersLogs(String emplacementFichiersLogs) {
		this.emplacementFichiersLogs = emplacementFichiersLogs;
	}

	/**
	 * active ou désactive un fichier.
	 * 
	 * @param nom
	 *            le nom du fichier a activer
	 * @param etat
	 *            true s'il faut l'activer, false sinon
	 */
	public void setEnabledFichier(String nom, boolean etat) {
		final Fichier f = getFichier(nom);
		if (etat) {
			f.setEtatFichier(ETAT.EN_ATTENTE);
			reactiverDependancesAncetres(f);
		} else {
			f.setEtatFichier(ETAT.DESACTIVE_UTILISATEUR);
			desactiverDependances(f);
		}

	}

	private void desactiverDependances(Fichier f) {
		for (final Fichier dependant : f.getFichiersQuiDependentDeNous()) {
			if (dependant.getEtatFichier() == ETAT.EN_ATTENTE) {
				dependant.setEtatFichier(ETAT.DESACTIVE_UTILISATEUR);
				desactiverDependances(dependant);
			}

		}

	}

	private void reactiverDependancesAncetres(Fichier f) {
		for (final Fichier onEnDepend : f.getFichiersDontOnDepend()) {
			onEnDepend.setEtatFichier(ETAT.EN_ATTENTE);
			reactiverDependancesAncetres(onEnDepend);
		}
	}

	/**
	 * @return le nombre de fichier non-desactivés dans le schema
	 */
	public int getNbFichiersActifs() {
		int nb = 0;
		for (final Fichier f : fichiers) {
			if (f.getEtatFichier() == ETAT.EN_ATTENTE) {
				nb++;
			}
		}
		return nb;
	}

	/**
	 * @return l'encodage des fichier
	 */
	public String getEncoding() {
		return encoding;
	}

	/**
	 * Désigne l'encodage des fichiers.
	 * 
	 * @param encoding string qui désigne l'encodage des fichiers. Voir{@link Charset}.
	 */
	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	/**
	 * Zip les fichiers de log dans le fichier désigné.
	 * 
	 * @param outputZip		le fichier destination.
	 * @throws IOException	si une erreur d'écriture a lieu.
	 */
	public void zipLogFiles(File outputZip) throws IOException {
		LogFilesZipper.zip(outputZip, listCheminFichiersLog);
	}

	/**
	 * @param b
	 *            true si on veut afficher le bouton Exporter les logs dans
	 *            l'interface utilisateur
	 */
	public void setAfficherExportLogs(boolean b) {
		afficherExportLogs = b;

	}

	/**
	 * @return true si on veut afficher le bouton Exporter les logs dans
	 *         l'interface utilisateur
	 */
	public boolean isAfficherExportLogs() {
		return afficherExportLogs;
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		String s = "";

		for (final Fichier f : fichiers) {
			s += f.getNom() + " : " + f.getEtatFichier() + "\n";
		}
		return s;
	}

	/**
	 * @return le separateur de champ utilisé.
	 */
	public SeparateurChamps getSeparateurChamp() {
		return separateurChamp;
	}

	/**
	 * @param sep le séparateur de champs à utiliser. Voir {@link SeparateurChamps}.
	 */
	public void setSeparateurChamp(SeparateurChamps sep) {
		this.separateurChamp = sep;
	}

	/**
	 * @param sep le séparateur de décimales à utiliser. Voir {@link SeparateurDecimales}.
	 */
	public void setSeparateurDecimales(SeparateurDecimales sep) {
		this.separateurDecimales = sep;
	}

	/**
	 * @return le separateur des décimales.
	 */
	public SeparateurDecimales getSeparateurDecimales() {
		return separateurDecimales;
	}

	/**
	 * @return les catégories du schéma.
	 */
	public Categories getCategories() {
		return categories;
	}

	/**
	 * Retire le fichier du schéma.
	 * 
	 * @param f
	 *            le fichier à retirer.
	 */
	public void removeFichier(Fichier f) {
		fichiers.remove(f);

	}

	/**
	 * @return le nombre total de fichiers dans le schéma.
	 */
	public int getNbFichiers() {
		return fichiers.size();
	}

}
