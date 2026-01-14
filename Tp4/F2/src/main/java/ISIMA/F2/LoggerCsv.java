package ISIMA.F2;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Création d'un moyen d'enregistrer les différentes éxecution du programme dans un fichier csv.
 * Pour faire des analyses graphiques à l'aide de python.
 */
public class LoggerCsv {

  private FileWriter writer;
  private final StringBuilder sb;

  /**
   *Crée le fichier dans lequel seront stockées les données et ajoute l'entête du fichier.
   *
   * @param fileName : Nom du fichier ou va etre ajoute les donnees
   * @param dureeSimu : Temps total de la simulation en mois
   *
   */
  public LoggerCsv(String fileName, int dureeSimu, int numeroSimu) {
    sb = new StringBuilder();

    try {
      String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd|MM_HH:mm"));
      fileName += "_" + dureeSimu + "_mois_" + timestamp + "_" + numeroSimu + ".csv";
      writer = new FileWriter(fileName);

      // writer.write("Timestamp : "+ timestamp + "\n");
      sb.append("Timestamp : ");
      sb.append(timestamp);
      sb.append("\n");

      // writer.write("Mois,Lapins vivants,Nombre de lapin juvéniles morts au total,Nombre de lapin enfant mort au total,Nombre de lapin adulte mort au total,Total de lapins morts,Nombre de naissance par mois,Nombre de lapin juvéniles morts par mois,Nombre de lapin enfant mort par mois,Nombre de lapin adulte mort par mois,Nombre de lapin mort en 1 mois\n");
      sb.append("Mois,Lapins vivants,Nombre de lapin juvéniles morts au total,Nombre de lapin enfant mort au total,Nombre de lapin adulte mort au total,Total de lapins morts,Nombre de naissance par mois,Nombre de lapin juvéniles morts par mois,Nombre de lapin enfant mort par mois,Nombre de lapin adulte mort par mois,Nombre de lapin mort en 1 mois\n");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   *Cette fonction ajoute les données de la simulation chaque mois dans le fichier crée.
   *
   * @param mois : indique le numero du mois où en est la simulation
   * @param vivants : indique le nombre de lapins qui sont vivants à l'instant t
   * @param mortsTot : indique le nombre de lapins morts
   * @param mortsEnfants : indique le nombre de lapins non mature morts
   * @param mortsAdulte : indique le nombre de lapins mature morts
   * @param mortsBebe : indique le nombre de lapins de moins de 1 mois morts
   */
  public void logMois(int mois, int vivants, int mortsTot, int mortsBebeTot, int mortsEnfantsTot, int mortsAdulteTot, int naissanceMois, int mortsBebe, int mortsEnfants, int mortsAdulte, int mortsParMois) {
    // try {
    // StringBuilder sb = new StringBuilder();

    sb.append(mois);
    sb.append(",");
    sb.append(vivants);
    sb.append(",");
    sb.append(mortsBebeTot);
    sb.append(",");
    sb.append(mortsEnfantsTot);
    sb.append(",");
    sb.append(mortsAdulteTot);
    sb.append(",");
    sb.append(mortsTot);
    sb.append(",");
    sb.append(naissanceMois);
    sb.append(",");
    sb.append(mortsBebe);
    sb.append(",");
    sb.append(mortsEnfants);
    sb.append(",");
    sb.append(mortsAdulte);
    sb.append(",");
    sb.append(mortsParMois);
    sb.append("\n");

    // writer.write(mois + "," + vivants + "," + mortsBebeTot + "," + mortsEnfantsTot + "," + mortsAdulteTot + "," + mortsTot +","+naissanceMois+","+ mortsBebe + "," + mortsEnfants + "," + mortsAdulte + ","+mortsParMois+ "\n");
    // } catch (IOException e) {
    //    e.printStackTrace();
    // }
  }

  /**
   * Cette fonction ferme le fichier qui a été ouvert initialement.
   */
  public void close() {
    try {
      writer.write(sb.toString());
      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
