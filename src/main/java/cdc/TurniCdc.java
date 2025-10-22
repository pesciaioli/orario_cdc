package cdc;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class TurniCdc extends Application {
	Label lStato = new Label("Seleziona il file");
	Label lFileProff = new Label("...");
	TreeItem<String> classi;
	TreeItem<String> classeAttuale;
	ListView<String> lCoppie = new ListView<>();
	ArrayList<ArrayList<String>> elenco = new ArrayList<>();
	ArrayList<String> coppie = new ArrayList<>();
	Button avviaScansione;
	Button genera;
	Button rimuovi;
	Button bCoppia;
	TextField tCoppia = new TextField();

	public void genera() {
		
	}
	
	public void confronta() {
		String coppia = tCoppia.getText();
		Pattern pattern = Pattern.compile("(.*)(\\d[a-zA-Z]+)(.*)(\\d[a-zA-Z]+)(.*)");
		Matcher matcher = pattern.matcher(coppia);
		if (!matcher.find()) {
			lStato.setText("formato: 1dh-4f");
			return;
		}
		String classe1 = matcher.group(2).toUpperCase();
		String classe2 = matcher.group(4).toUpperCase();
		StringBuffer messaggio = new StringBuffer();
		int i, j, prof;
		for(i = 0; i < elenco.size() && !elenco.get(i).get(0).equals(classe1); i++);
		if(i == elenco.size()) {
			lStato.setText("classe di sinistra non trovata");
			return;
		}
		for(j = 0; j < elenco.size() && !elenco.get(j).get(0).equals(classe2); j++);
		if(j == elenco.size()) {
			lStato.setText("classe di destra non trovata");
			return;
		}
		for(prof = 0; prof < elenco.get(j).size(); prof++) {
			if(elenco.get(i).contains(elenco.get(j).get(prof))) {
				messaggio.append("  ");
				messaggio.append(elenco.get(j).get(prof));
			}
		}
		if(messaggio.length() > 0) {
			messaggio.insert(0, "Conflitto: ");
			lStato.setText(messaggio.toString());
		} else {
			lStato.setText("Classi indipendenti");
			lCoppie.getItems().add(classe1 + " - " + classe2);
			rimuovi.setDisable(false);
		}
	}
	
	public void rimuovi() {
		int i = lCoppie.getSelectionModel().getSelectedIndex();
		if(i == -1) {
			return;
		}
		lStato.setText("Rimossa coppia: " + lCoppie.getSelectionModel().getSelectedItem() + ", posizione : " + (i + 1));
		lCoppie.getItems().remove(i);
		//rimuovi.setDisable(true);
	}

	public void carica(Stage fin) {
		FileChooser fc = new FileChooser();
		fc.setInitialDirectory(new File(System.getProperty("user.home"), "Downloads"));
		fc.setTitle("File dei CdC");
        File file = fc.showOpenDialog(fin);
        if(file == null) {
        	lStato.setText("File non selezionato");
        	return;
        }
        lFileProff.setText(file.getName());
	    PDDocument documento = null;
	    String[] righe = null;
		try {
			documento = PDDocument.load(file);
		    PDFTextStripper stripper = new PDFTextStripper();
		    righe = stripper.getText(documento).split("\n");
		    documento.close();
		} catch (IOException e) {
			e.printStackTrace();
			lStato.setText("Errore in decodifica");
			return;
		}
		String regex = " \\.+ ";
		Pattern pattern = Pattern.compile(regex);
		String classe = "";
		ArrayList<String> proff = null;
		int lunghezzaRiga, posNome;
		for(int numRiga = 0; numRiga < righe.length; numRiga++) {
		   lunghezzaRiga = righe[numRiga].length();
		   //se è una classe, inseriscila e prendi gli insegnanti
		   if(lunghezzaRiga > 8 && righe[numRiga].indexOf("Classe ") == 0) {
			   //elimino eventuali duplicati
			   if(proff != null && proff.size() > 1 ) {
			      Collections.sort(proff.subList(1, proff.size()));
			      for(int i = 1; i < proff.size() - 1; i++) {
			    	  if(proff.get(i).equals(proff.get(i + 1))) {
			    		  proff.remove(i + 1);
			    		  i--; //se ce ne sono più di 2 evito di fare il passo avanti
			    	  }
			      }
			   }
			   //controllo se la riga contiene altre informazioni oltre la classe
			   if(righe[numRiga].indexOf(' ', 7) != -1) {
			      classe = righe[numRiga].substring(7, righe[numRiga].indexOf(' ', 7)).trim();
			   } else {
				   classe = righe[numRiga].substring(7).trim();
			   }
			   classeAttuale = new TreeItem<String> (classe);
			   classi.getChildren().add(classeAttuale);
			   proff = new ArrayList<>();
			   proff.add(classe);
			   elenco.add(proff);
			   //presenza di un insegnante sulla stessa riga della classe?
			   if((posNome = righe[numRiga].indexOf("> Doce.")) > 0 && lunghezzaRiga - posNome > 11) {
				   righe[numRiga] = righe[numRiga].substring(posNome + 7);
			   } else {
			      continue;
			   }
		   }
		   //aggiungi un insegnante
		   if(righe[numRiga].lastIndexOf("h00") > 0) {
			   Matcher matcher = pattern.matcher(righe[numRiga]);
			   if(matcher.find()) {
				   String punti = matcher.group();
				   righe[numRiga] = righe[numRiga].substring(righe[numRiga].indexOf(punti) + punti.length()).trim();
			   }
			   righe[numRiga] = righe[numRiga].substring(0, righe[numRiga].length() - 5);
			   if(righe[numRiga].lastIndexOf(" (PP)") > 0) {
				   righe[numRiga] = righe[numRiga].substring(0, righe[numRiga].length() - 4);
			   }
			   righe[numRiga] = righe[numRiga].trim();
			   proff.add(righe[numRiga]);
			   TreeItem<String> professore = new TreeItem<String> (righe[numRiga]);            
	           classeAttuale.getChildren().add(professore);
		   }
		}
		bCoppia.setDisable(false);
		avviaScansione.setDefaultButton(false);
		bCoppia.setDefaultButton(true);
	}

	//TO-DO: Aggiungere giorno libero
	public void start(Stage f) {
		bCoppia = new Button("confronta");
		bCoppia.setGraphic(new ImageView("confronta.gif"));
		bCoppia.setMinSize(100, 50);
		bCoppia.setOnAction(e -> confronta());
		bCoppia.setDisable(true);
		classi = new TreeItem<String> ("Classi", new ImageView("classe.gif"));
		TreeView<String> albero = new TreeView<>(classi);
		albero.setMinWidth(300);
        classi.setExpanded(true);
		GridPane p = new GridPane();
		//p.setStyle("-fx-background-image: url(\"scuola20.png\")");
		p.setHgap(12); p.setVgap(10); p.setPadding(new Insets(16));
		Label lDocenti = new Label("File dei docenti");
		p.add(lDocenti, 0, 0);
		p.add(lFileProff, 1, 0);
		p.add(new Label("Classi:"), 2, 0);
		p.add(tCoppia, 2, 1);
		p.add(bCoppia, 2, 2);
		avviaScansione = new Button("Scansiona i proff nel file");
		avviaScansione.setDefaultButton(true);
		avviaScansione.setGraphic(new ImageView("classe.gif"));
		avviaScansione.setMinSize(100, 50);
		genera = new Button("Genera proposta");
		avviaScansione.setOnAction(e -> carica(f));
		genera.setOnAction(e -> genera());
		genera.setDisable(true);
		rimuovi = new Button("Rimuovi");
		rimuovi.setDisable(true);
		rimuovi.setOnAction(e -> rimuovi());
		p.add(rimuovi, 3, 3);
		p.add(avviaScansione, 0, 1);
		p.add(genera, 0, 2);
		p.add(albero, 0, 3, 2, 1);
		p.add(lCoppie, 2, 3);
		p.add(lStato, 0, 4, 3 , 1);
		f.setScene(new Scene(p, 700, 400));
		f.setTitle("Turni");
		f.show();
	}

	public static void main(String[] args) {
		launch(args);
	}
}
