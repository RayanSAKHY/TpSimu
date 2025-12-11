package ISIMA.F2.assets;
import ISIMA.F2.LoggerCSV;

import org.apache.commons.math3.random.MersenneTwister;

import java.util.LinkedList;
import java.util.Iterator;

/**
 * Implementation de toutes les fonctions pour gérer la simulation
 */
public class SimuUtils {

    /**
     * Suivi du nombre de lapins total qui sont né durant toute la simulation
     */
    private static int nbLapinTot=0;
    /**
     * Suivi du nombre de lapins total qui sont mort durant la simulation
     */
    private static int nbLapinMort=0;


    /**
     *Cette fonction sert à simuler des tirages aléatoires avec des probabilités donnés en argument
     * @param poids : Tableau qui stocke la pondération voulue pour le tirage aléatoire
     * @param rng : Instanciation de Mersenne Twister
     * @return : Renvoie l'index qui correspond à la probabilité
     */
    private static int tirageProbaSelonPoids(double[] poids,MersenneTwister rng ) {
        double total = 0;
        double nbAlea;
        double cumul = 0;
        int i;


        for (double p : poids) {
            total += p;
        }

        nbAlea = rng.nextDouble() * total;

        for (i = 0; i < poids.length; i++) {
            cumul += poids[i];
            if (nbAlea < cumul) return i;
        }
        return poids.length - 1;
    }


    /**
     * Cette fonction renvoie le nombre de portées qu'une lapine peut avoir en un an
     * @param rng : Instanciation de Mersenne Twister
     * @return : Renvoie le nombre de portées qu'une lapine peut avoir
     */
    private static int nombrePortee(MersenneTwister rng) {
        int[] possibilite = {3, 4, 5, 6, 7, 8, 9};
        double[] poids = {0.05, 0.1, 0.2, 0.3, 0.2, 0.1, 0.05};
        return possibilite[tirageProbaSelonPoids(poids,rng)];
    }

    /**
     * Cette fonction renvoie le nombre d'enfants qu'une lapine peut avoir à chaque portée
     * @param rng : Instanciation de Mersenne Twister
     * @return : renvoie le nombre d'enfants qu'une lapine peut avoir
     */
    private static int nombreEnfants(MersenneTwister rng) {
        int[] possibilite = {3, 4, 5, 6};
        double[] poids = {0.25, 0.25, 0.25, 0.25};
        return possibilite[tirageProbaSelonPoids(poids,rng)];
    }

    /**
     * Initialise la population de lapin composé de nbPaire de couple de lapin mature
     * @param nbPaire : Entier qui symbolise le nombre de couples de lapin
     * @param population : Instanciation d'une liste chainée servant à stocker les lapins
     * @param rng : Instanciation de Mersenne Twister
     * @return : renvoie la liste chainée modifiée avec les couples de lapins
     */
    public LinkedList<Lapin> popInit(int nbPaire, LinkedList<Lapin> population,MersenneTwister rng){
        int i ;
        population.clear();
        nbLapinMort = 0;
        nbLapinTot = 0;
        for (i=0; i<nbPaire ; i++){
            population.add(new Lapin(6,0,5) );
            population.add(new Lapin(6,1,5) );
            nbLapinTot+=2;
            population.get(2*i+1).nbPorteeRestante=nombrePortee(rng);
        }

        return population;
    }

    /**
     * Simule des probabilités de mort de lapin plus domestiques
     * @param lapin : Instanciation de lapin
     * @param nbAlea : Nombre aléatoire qui sert à déterminer si le lapin en argument est mort
     * @return : True si le lapin est mort a la fin du mois ou false sinon
     */
    private boolean estMortV1(Lapin lapin, double nbAlea) {
        int age = lapin.age;
        boolean sortie = false;
        if (lapin.estMature == 1) {
            if (age >= 180) sortie = true;
            else if (age >= 168) sortie = nbAlea < 0.126;
            else if (age >= 156) sortie = nbAlea < 0.0997;
            else if (age >= 144) sortie = nbAlea < 0.0847;
            else if (age >= 132) sortie = nbAlea < 0.0698;
            else if (age >= 120) sortie = nbAlea < 0.0561;
            else sortie = nbAlea < 0.0426;
        } else if (lapin.estMature == 0) sortie = nbAlea < 0.0649;
        return sortie;
    }

    /**
     * Simule des probabilités de mort de lapin plus sauvage
     * @param lapin : Instanciation de lapin
     * @param nbAlea : Nombre aléatoire qui sert à determiner si le lapin en argument est mort
     * @return : True si le lapin est mort a la fin du mois ou false sinon
     */
    private boolean estMortV2(Lapin lapin, double nbAlea) {
        int age = lapin.age;
        boolean sortie = false;

        if (lapin.estMature == 1) {
            if (age >=180) sortie = true;
            else if (age >=168) sortie = nbAlea < 0.126;
            else if (age >=156) sortie = nbAlea < 0.0997;
            else if (age >= 144) sortie = nbAlea < 0.0847;
            else if (age >= 132) sortie = nbAlea < 0.0698;
            else if (age >= 120) sortie = nbAlea < 0.0561;
            else if (age >= 108) sortie = nbAlea <0.0426;
            else if (age >= 96) sortie = nbAlea <0.0296;
            else sortie = nbAlea < 0.0184;
        }
        else if (lapin.estMature == 0){
            if (age < 1 ) sortie = nbAlea < 0.8;
            else sortie = nbAlea < 0.0561;
        }

        return sortie;
    }

    /**
     * Retire les lapins de la liste s'ils sont morts
     * @param population : Liste chainée qui contient les lapins
     * @param rng : Instanciation de Mersenne Twister
     * @return : renvoie la liste sans les lapins morts
     */
    public LinkedList<Lapin> verifMort(LinkedList<Lapin> population,MersenneTwister rng) {
        //System.out.println("test entre mort : " +l.get(3).age);
        Iterator<Lapin> it = population.iterator();
        while (it.hasNext()) {
            Lapin lapin = it.next();
            if (estMortV2(lapin, rng.nextDouble())) {
                it.remove();
                lapin = null;
                nbLapinMort++;
            }
        }
        //System.out.println("taille liste l : "+l.size());
        return population;
    }

    /**
     * Met à jour la liste en verifiant le nombre de portées par ans, la maturité des nouveaux nés et l'âge des lapins en mois
     * @param rng : Instanciation de Mersenne Twister
     * @param population : Instanciation d'une liste chainée pour stocker les lapins
     * @return : renvoie la liste chainée avec les lapins modifiés
     */
    private LinkedList<Lapin> miseAjourListe(MersenneTwister rng,LinkedList<Lapin> population) {
        double[] poids = {0.25, 0.30, 0.45};
        int[] possibilite = {5, 6, 7};
        for (Lapin lapin : population){
            if (lapin.sexe == 1 && lapin.age % 12 == 0){
                lapin.nbPorteeRestante=nombrePortee(rng);
            }

            lapin.age+=1;

            if (lapin.estMature == 0) {
                if (lapin.age >= 8) lapin.estMature = 1;
                else if (lapin.age >= possibilite[tirageProbaSelonPoids(poids, rng)]) {
                    lapin.estMature = 1;
                }
            }
        }
        return population;
    }

    /**
     * Fait naitre les nouveaux lapins selon le nombre de portées et le nombre de lapins par portées
     * @param population : Instanciation d'une liste chainée pour stocker les lapins
     * @param rng : Instanciation  de Mersenne Twister
     * @return : Renvoie la liste chainée en argument avec les nouveaux lapins
     */
    public LinkedList<Lapin> naissance(LinkedList<Lapin> population, MersenneTwister rng) {
        boolean malePresent = false;
        int i;
        Lapin currentLapin;
        double[] poids = {0.25, 0.30, 0.45};
        int[] possibilite = {5, 6, 7};

        Iterator<Lapin> it = population.iterator();
        while (it.hasNext() && !malePresent){
            currentLapin = it.next();
            if (currentLapin.sexe == 0 && currentLapin.estMature == 1) {
                malePresent = true;
            }
        }
        population =miseAjourListe(rng,population);
        LinkedList<Lapin> nouveaux = new LinkedList<>(population);
        if (malePresent) {
            for (Lapin lapin : population) {

                if (lapin.sexe == 1 && lapin.estMature == 1 && lapin.nbPorteeRestante > 0) {
                    if (lapin.nbEnfantEnGestation == 0) {
                        lapin.nbPorteeRestante -= 1;
                        lapin.nbEnfantEnGestation = nombreEnfants(rng);
                    } else {
                        for (i = 0; i < lapin.nbEnfantEnGestation; i++) {
                            nouveaux.add(new Lapin(0, rng.nextInt(2), possibilite[tirageProbaSelonPoids(poids, rng)]));
                            nbLapinTot++;
                        }
                        lapin.nbEnfantEnGestation = 0;
                    }
                }
            }
        }
        population.clear();
        population = null;
        //System.out.println("taille liste nouveaux : "+nouveaux.size());

        return nouveaux;
    }

    /**
     * Affiche le nombre de lapins actuels
     * @param nbMois : Numero du mois simulé
     * @param population : Instanciation d'une liste chainée qui contient les lapins
     */
    public void nombreActuelLapin(int nbMois,LinkedList<Lapin> population){
        System.out.println("Nombre de lapins dans la population au mois "+nbMois+" : "+population.size());
        System.out.println("Pour "+ nbLapinMort +" lapins mort et "+nbLapinTot + " naissances de lapins.\n");
    }

    /**
     * Simule plusieurs mois
     * @param nbMois : Durée de la simulation
     * @param population : Instanciation d'une liste chainée pour stocker les lapins
     * @param rng : instanciation de Mersenne Twister
     * @return : Renvoie la liste chainée en argument après toute la durée de simulation
     */
    public LinkedList<Lapin> Simulation(int nbMois, LinkedList<Lapin> population, MersenneTwister rng){
        int i;
        LoggerCSV logger = new LoggerCSV("logs/SimulationLapin",nbMois);
        for (i=0; i<nbMois; i++){
            population = simuler1Mois(population, rng);
            nombreActuelLapin(i+1,population);

            logger.logMois(i+1, nbLapinTot-nbLapinMort, nbLapinMort);

        }
        logger.close();
        return population;
    }

    /**
     * Simule in mois
     * @param population : Instanciation d'une liste chainée qui sert à stocker les lapins
     * @param rng : Instanciation de Mersenne Twister
     * @return : Renvoie la liste chainée modifiée après 1 mois de simulation
     */
    public LinkedList<Lapin> simuler1Mois(LinkedList<Lapin> population, MersenneTwister rng){
        population = naissance(population,rng);
        population = verifMort(population,rng);
        return population;
    }
}
