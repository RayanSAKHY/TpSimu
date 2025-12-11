package ISIMA.F2;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Création d'un moyen d'enregistrer les différentes éxecution du programme dans un fichier csv pour faire des analyses graphiques à l'aide d'excel
 */
public class LoggerCSV {

    private FileWriter writer;

    /**
     *Crée le fichier dans lequel seront stockées les données et ajoute l'entête du fichier
     * @param fileName : Nom du fichier ou va etre ajoute les donnees
     * @param dureeSimu : Temps total de la simulation en mois
     *
     */
    public LoggerCSV(String fileName, int dureeSimu) {
        try {

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd|MM_HH:mm"));
            fileName +="_" + dureeSimu + "_mois_"+ timestamp+".csv";
            writer = new FileWriter(fileName);
            writer.write("Timestamp : "+ timestamp + "\n");
            writer.write("Mois,Lapins vivants,Lapins morts\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *Cette fonction ajoute les données de la simulation du mois actuel dans le fichier créé initialement
     * @param mois : indique le numero du mois où en est la simulation
     * @param vivants : indique le nombre de lapins qui sont vivants à l'instant t
     * @param morts : indique le nombre de lapins morts
     *
     */
    public void logMois(int mois, int vivants, int morts) {
        try {
            writer.write(mois + "," + vivants + "," + morts + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Cette fonction ferme le fichier qui a été ouvert initialement
     */
    public void close() {
        try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
