package me.petrolingus;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Properties;

public class Controller {

    public TableView<ObservableList<String>> table;


    public Hyperlink hyperlinkPhonebook;


    public VBox searchParametersBox;

    public TextField lastNameField;

    public TextField firstNameField;

    public TextField middleNameField;

    public TextField phoneNumberField;

    public TextField providerField;


    public VBox toolBox;

    public Button addButton;


    private Connection connection;

    private Properties columnMappings;

    private int selectedLink = 0;


    public void initialize() throws SQLException {

        String url = "jdbc:sqlserver://localhost:1433;databaseName=TestDB";
        String user = "SA";
        String password = "H#MTlWikoDOfbB1#";
        connection = DriverManager.getConnection(url, user, password);

        columnMappings = new Properties();

        try (FileReader reader = new FileReader("columnMappings.properties")) {
            columnMappings.load(reader);
        } catch (IOException e) {
            e.printStackTrace();
        }

        generateTable("select * from person;");
    }

    public void onLinkPressed(ActionEvent event) throws SQLException {

        Hyperlink hyperlink = ((Hyperlink) event.getSource());

        switch (hyperlink.getId()) {
            case "hyperlinkPeople":
                selectedLink = 0;
                generateTable("select * from person;");
                break;
            case "hyperlinkPhones":
                selectedLink = 1;
                generateTable("select phone_number.id as id, phone, type, name from phone_number join provider on provider.id = provider;");
                break;
            case "hyperlinkProviders":
                selectedLink = 2;
                generateTable("select * from provider;");
                break;
            case "hyperlinkPhonebook":
                selectedLink = 3;
                generateTable("select first_name, middle_name, last_name, phone, type, name from phone_contact \n" +
                        "\tjoin person on person.id = person_id\n" +
                        "\tjoin phone_number on phone_number.id = phone_number_id\n" +
                        "\tjoin provider on provider.id = phone_number.provider;");
                break;
        }

        searchParametersBox.setDisable(selectedLink != 3);
        toolBox.setDisable(selectedLink == 3);
    }

    private void generateTable(String sql) throws SQLException {

        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(sql);

        table.getColumns().clear();
        for (int i = 0; i < resultSet.getMetaData().getColumnCount(); i++) {
            String dbColumnName = resultSet.getMetaData().getColumnName(i + 1);
            String columnName = columnMappings.getProperty(dbColumnName);
            if (columnName != null) {
                int finalI = i;
                TableColumn<ObservableList<String>, String> column = new TableColumn<>(columnName);
                column.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().get(finalI)));
                table.getColumns().add(column);
            }
        }

        ObservableList<ObservableList<String>> data = FXCollections.observableArrayList();

        while (resultSet.next()) {
            ObservableList<String> row = FXCollections.observableArrayList();
            for (int i = 1; i <= resultSet.getMetaData().getColumnCount(); i++) {
                row.add(resultSet.getString(i));
            }
            data.add(row);
        }

        table.getItems().clear();
        table.setItems(data);
    }

    public void onKeyTyped() throws SQLException {

        String query = "select first_name, middle_name, last_name, phone, type, name from phone_contact\n" +
                "    join person on person.id = person_id\n" +
                "    join phone_number on phone_number.id = phone_number_id\n" +
                "    join provider on provider.id = phone_number.provider\n" +
                "    where";

        String lastName = " last_name like N'%" + lastNameField.getText() + "%'";
        String firstName = "  first_name like N'%" + firstNameField.getText() + "%'";
        String middleName = "  middle_name like N'%" + middleNameField.getText() + "%'";
        String phoneNumber = "  phone like N'%" + phoneNumberField.getText() + "%'";
        String provider = "  name like N'%" + providerField.getText() + "%'";

        String join = String.join(" and ", lastName, firstName, middleName, phoneNumber, provider);

        if (selectedLink == 3) {
            generateTable(query + join);
        }
    }

    public void onAddButton() throws SQLException {

        TextField lastNameField = new TextField();
        lastNameField.setPromptText("Last Name");

        TextField firstNameField = new TextField();
        firstNameField.setPromptText("First Name");

        TextField middleNameField = new TextField();
        middleNameField.setPromptText("First Name");

        TextField birthday = new TextField();
        birthday.setPromptText("birthday");

        TextField address = new TextField();
        address.setPromptText("address");

        TextField comment = new TextField();
        comment.setPromptText("comment");

        VBox vbox = new VBox();
        vbox.setSpacing(8);
        vbox.getChildren().addAll(lastNameField, firstNameField, middleNameField, birthday, address, comment);

        Dialog<ArrayList<String>> dialog = new Dialog<>();
        dialog.setTitle("Add Dialog");
        dialog.setHeaderText("Creating a new row");

        // Set the button types.
        ButtonType loginButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);
        dialog.getDialogPane().setContent(vbox);

        // Convert the result to a username-password-pair when the login button is clicked.
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                ArrayList<String> result = new ArrayList<>();
                result.add(lastNameField.getText());
                result.add(firstNameField.getText());
                result.add(middleNameField.getText());
                result.add(birthday.getText());
                result.add(address.getText());
                result.add(comment.getText());

                return result;
            }
            return null;
        });

        Optional<ArrayList<String>> result = dialog.showAndWait();

        Statement statement = connection.createStatement();

        result.ifPresent(p -> {
            ArrayList<String> arrayList = result.get();

            String query = "insert into person values (N'" + arrayList.get(0) + "', N'" +
                    arrayList.get(1) + "', N'" + arrayList.get(2) + "', '" + arrayList.get(3) + "', N'" +
                    arrayList.get(4) + "', N'" + arrayList.get(5) + "')";

            try {
                statement.execute(query);
            } catch (SQLException e) {
                e.printStackTrace();
            }

            try {
                generateTable("select * from person;");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }
}
