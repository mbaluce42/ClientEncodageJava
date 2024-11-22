package VIEW;

import MODEL.networking.Prot_OBEP;
import MODEL.networking.ResultatOBEP;
import MODEL.networking.SocketManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.net.Socket;

public class MainWindowClientBookEncoder extends JFrame
{
    private JPanel mainPanel;
    private JTextField titleField, isbnField;
    private JSpinner pagesSpinner, yearSpinner, stockSpinner;
    private JSpinner priceSpinner;
    private JComboBox<String> authorsCombo, subjectsCombo;
    private JTable booksTable;
    private DefaultTableModel tableModel;
    private JButton clearButton, addBookButton, addAuthorButton, addSubjectButton;
    private JMenuItem loginItem, logoutItem, quitItem;
    private int bookId = 1;
    private boolean isLoggedIn = false;
    private String portServeur = "50000";
    private String ipServeur = "192.168.163.128";
    private Socket clientSocket = null;
    private Prot_OBEP protocol = null;
    private String IdEmployeeConnected="0";

    public MainWindowClientBookEncoder()
    {
        setTitle("Application Encodage");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // Empêche la fermeture par défaut

        // Ajouter un listener pour gérer la fermeture de la fenêtre
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                handleLogout(); // Appeler handleLogout()
                dispose(); // Fermer la fenêtre
                System.exit(0); // Terminer le programme
            }
        });

        // Configuration des listeners pour le menu
        loginItem.addActionListener(e -> handleLogin());
        logoutItem.addActionListener(e -> handleLogout());
        quitItem.addActionListener(e -> handleQuit());

        // Configuration des listeners pour les boutons
        clearButton.addActionListener(e -> handleClear());
        addBookButton.addActionListener(e -> handleAddBook());
        addAuthorButton.addActionListener(e -> handleAddAuthor());
        addSubjectButton.addActionListener(e -> handleAddSubject());

        // Configuration des spinners
        pagesSpinner.setModel(new SpinnerNumberModel(0, 0, 10000, 1));
        yearSpinner.setModel(new SpinnerNumberModel(1900, 1800, 2100, 1));
        priceSpinner.setModel(new SpinnerNumberModel(0.0, 0.0, 1000.0, 0.01));
        stockSpinner.setModel(new SpinnerNumberModel(0, 0, 1000, 1));

        // Configuration de la table
        createBooksTable();

        // Ajout du panel principal
        setContentPane(mainPanel);
        pack();
        setLocationRelativeTo(null);


        // État initial (déconnecté)
        updateLoginState(false);

        // Connexion au serveur
        try
        {
            clientSocket = SocketManager.createClientSocket(ipServeur, portServeur);
            protocol = new Prot_OBEP(clientSocket);
            System.out.println("Connexion au serveur réussie");
        } catch (IOException e) {
            e.printStackTrace();
        }

        setVisible(true);
    }

    private void createBooksTable()
    {
        String[] columnNames = {"Id", "Titre", "Auteur", "Sujet", "ISBN", "Pages", "Année", "Prix", "Stock"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column)
            {
                return false;
            }
        };
        booksTable.setModel(tableModel);

        // Configuration des largeurs des colonnes
        int[] columnWidths = {35, 250, 200, 200, 150, 50, 50, 50, 40};
        for (int i = 0; i < columnWidths.length; i++)
        {
            booksTable.getColumnModel().getColumn(i).setPreferredWidth(columnWidths[i]);
        }
    }

    private void updateLoginState(boolean loggedIn)
    {
        isLoggedIn = loggedIn;
        clearButton.setEnabled(loggedIn);
        addBookButton.setEnabled(loggedIn);
        addAuthorButton.setEnabled(loggedIn);
        addSubjectButton.setEnabled(loggedIn);
        loginItem.setEnabled(!loggedIn);
        logoutItem.setEnabled(loggedIn);

        if (!loggedIn)
        {
            bookId = 1;
            clearTable();
        }
    }

    private void handleLogin()
    {
        String login = JOptionPane.showInputDialog(this, "Login ?", "Entrée en session", JOptionPane.QUESTION_MESSAGE);
        String password = JOptionPane.showInputDialog(this, "Password ?", "Entrée en session", JOptionPane.QUESTION_MESSAGE);

        if (login != null && password != null)
        {
            try
            {
                ResultatOBEP resultat = protocol.OBEP_Op("LOGIN#" + login + "#" + password);
                if (resultat.isSuccess())
                {
                    JOptionPane.showMessageDialog(this, resultat.getMessage(), "Succès", JOptionPane.INFORMATION_MESSAGE);
                    updateLoginState(true);
                    loadAuthorsAndSubjects();
                    loadEncodedBookByEmployee(login);
                }
                else
                {
                    JOptionPane.showMessageDialog(this, "Erreur : " + resultat.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void handleLogout()
    {
        try
        {
            ResultatOBEP resultat = protocol.OBEP_Op("LOGOUT#");
            if (resultat.isSuccess())
            {
                JOptionPane.showMessageDialog(this, resultat.getMessage(), "Succès", JOptionPane.INFORMATION_MESSAGE);
                updateLoginState(false);
            }
            else
            {
                JOptionPane.showMessageDialog(this, "Erreur : " + resultat.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleQuit()
    {
        handleLogout();
    }

    private void handleAddAuthor()
    {
        String nom = JOptionPane.showInputDialog(this, "Nom ?", "Nouvel auteur", JOptionPane.QUESTION_MESSAGE);
        String prenom = JOptionPane.showInputDialog(this, "Prénom ?", "Nouvel auteur", JOptionPane.QUESTION_MESSAGE);
        String dateNaissance = JOptionPane.showInputDialog(this, "Date de naissance (yyyy-mm-dd) ?", "Nouvel auteur", JOptionPane.QUESTION_MESSAGE);

        if (nom != null && prenom != null && dateNaissance != null)
        {
            nom = Character.toUpperCase(nom.charAt(0)) + nom.substring(1);
            prenom = Character.toUpperCase(prenom.charAt(0)) + prenom.substring(1);

            try
            {
                ResultatOBEP resultat = protocol.OBEP_Op("ADD_AUTHOR#" + nom + "#" + prenom + "#" + dateNaissance);
                if (resultat.isSuccess())
                {
                    authorsCombo.addItem(prenom + " " + nom);
                    JOptionPane.showMessageDialog(this, resultat.getMessage(), "Succès", JOptionPane.INFORMATION_MESSAGE);
                }
                else
                {
                    JOptionPane.showMessageDialog(this, "Erreur : " + resultat.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void handleAddSubject()
    {
        String nom = JOptionPane.showInputDialog(this, "Nom ?", "Nouveau sujet", JOptionPane.QUESTION_MESSAGE);

        if (nom != null)
        {
            try {
                ResultatOBEP resultat = protocol.OBEP_Op("ADD_SUBJECT#" + nom);
                if (resultat.isSuccess())
                {
                    subjectsCombo.addItem(nom);
                    JOptionPane.showMessageDialog(this, resultat.getMessage(), "Succès", JOptionPane.INFORMATION_MESSAGE);
                }
                else
                {
                    JOptionPane.showMessageDialog(this, "Erreur : " + resultat.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void handleAddBook()
    {
        if (verifBookInput())
        {
            String auteur = authorsCombo.getSelectedItem().toString();
            String sujet = subjectsCombo.getSelectedItem().toString();
            String titre = titleField.getText();
            String isbn = isbnField.getText();
            int pages = (int) pagesSpinner.getValue();
            int annee = (int) yearSpinner.getValue();
            double prix = (double) priceSpinner.getValue();
            int stock = (int) stockSpinner.getValue();
            int idAuteur = 0;
            int idSujet = 0;

            try {
                ResultatOBEP resultat = protocol.OBEP_Op("GETID_AUTHOR#" + auteur);
                if (resultat.isSuccess())
                {
                    idAuteur = Integer.parseInt(resultat.getMessage().trim());
                }
                else
                {
                    JOptionPane.showMessageDialog(this, "Erreur : " + resultat.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                resultat = protocol.OBEP_Op("GETID_SUBJECT#" + sujet);
                if (resultat.isSuccess())
                {
                    idSujet = Integer.parseInt(resultat.getMessage().trim());
                }
                else
                {
                    JOptionPane.showMessageDialog(this, "Erreur : " + resultat.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                resultat = protocol.OBEP_Op("ADD_BOOK#" + idAuteur + "#" + idSujet + "#" + titre + "#" + isbn + "#" + pages + "#" + stock + "#" + prix + "#" + annee);
                if (resultat.isSuccess())
                {
                    Object[] rowData = {bookId, titre, auteur, sujet, isbn, pages, annee, prix, stock};
                    tableModel.addRow(rowData);
                    bookId++;
                    handleClear();
                    JOptionPane.showMessageDialog(this, resultat.getMessage(), "Succès", JOptionPane.INFORMATION_MESSAGE);

                    resultat=protocol.OBEP_Op("ADD_ENCODEDBOOK#"+IdEmployeeConnected+"#"+(bookId-1));
                    if (resultat.isSuccess()==false)
                    {
                        JOptionPane.showMessageDialog(this, "Erreur : " + resultat.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
                    }
                }
                else
                {
                    JOptionPane.showMessageDialog(this, "Erreur : " + resultat.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private boolean verifBookInput()
    {
        if (titleField.getText().isEmpty() || isbnField.getText().isEmpty() || (int) pagesSpinner.getValue() == 0 || (double) priceSpinner.getValue() == 0.0 || (int) yearSpinner.getValue() == 0 || (int) stockSpinner.getValue() == 0)
        {
            JOptionPane.showMessageDialog(this, "Veuillez remplir tous les champs", "Erreur", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    private void handleClear()
    {
        titleField.setText("");
        isbnField.setText("");
        pagesSpinner.setValue(0);
        priceSpinner.setValue(0.0);
        yearSpinner.setValue(1900);
        stockSpinner.setValue(0);
    }

    private void clearTable()
    {
        while (tableModel.getRowCount() > 0)
        {
            tableModel.removeRow(0);
        }
    }

    private void loadAuthorsAndSubjects()
    {
        try {
            // Charger les sujets
            ResultatOBEP resultat = protocol.OBEP_Op("GET_SUBJECTS#");
            if (resultat.isSuccess()) {
                for (String line : resultat.getMessage().split("\n")) {
                    String[] parts = line.split(";");
                    subjectsCombo.addItem(parts[1]);
                }
            } else {
                System.out.println("Erreur (sujets) : " + resultat.getMessage());
            }

            // Charger les auteurs
            resultat = protocol.OBEP_Op("GET_AUTHORS#");
            if (resultat.isSuccess()) {
                for (String line : resultat.getMessage().split("\n")) {
                    String[] parts = line.split(";", 4);
                    authorsCombo.addItem(parts[2] + " " + parts[1]);
                }
            } else {
                System.out.println("Erreur (auteurs) : " + resultat.getMessage());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void loadEncodedBookByEmployee(String login)
    {
        try
        {
            ResultatOBEP resultat = protocol.OBEP_Op("GETID_EMPLOYEE#" + login);
            if (resultat.isSuccess())
            {
                IdEmployeeConnected=resultat.getMessage().trim();
                System.out.println("IdEmployeeConnected:"+IdEmployeeConnected);

                resultat = protocol.OBEP_Op("GET_ENCODEDBOOKSBYEMPLOYEE#" + IdEmployeeConnected);
                if (resultat.isSuccess())
                {
                    for (String line : resultat.getMessage().split("\n"))
                    {
                        String[] parts = line.split(";");
                        String idBook=parts[2];

                        ResultatOBEP resultatBook = protocol.OBEP_Op("GET_BOOKBYID#" + idBook);
                        if (resultatBook.isSuccess())
                        {
                            String[] partsBook = resultatBook.getMessage().split(";");
                            String id = partsBook[0];
                            String idAuthor=partsBook[1];
                            String idSubject=partsBook[2];
                            String title = partsBook[3];
                            String isbn = partsBook[4];
                            String pages = partsBook[5];
                            String stock = partsBook[6];
                            String price = partsBook[7];
                            String year = partsBook[8];

                            //get author
                            String Author  = protocol.OBEP_Op("GET_AUTHORBYID#" + idAuthor).getMessage();
                            String[] authorParts = Author.split(";",4);
                            String authorLastName = authorParts[1];
                            String authorFirstName = authorParts[2];

                            //get subject
                            String Subject = protocol.OBEP_Op("GET_SUBJECTBYID#" + idSubject).getMessage();
                            String[] subjectParts = Subject.split(";",2);
                            String subjectName = subjectParts[1];

                            // add dans la table
                            Object[] rowData = {id, title, authorFirstName + " " + authorLastName, subjectName, isbn, pages, year, price, stock};
                            tableModel.addRow(rowData);
                            bookId++;
                        }
                    }
                }
            }
            else
            {
                JOptionPane.showMessageDialog(this, "Erreur : " + resultat.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args)
    {
        MainWindowClientBookEncoder frame = new MainWindowClientBookEncoder();
    }
}
