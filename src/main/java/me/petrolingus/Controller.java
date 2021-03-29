package me.petrolingus;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.Pair;
import javafx.util.StringConverter;

import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

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
                break;
            case "hyperlinkPhones":
                selectedLink = 1;
                break;
            case "hyperlinkProviders":
                selectedLink = 2;
                break;
            case "hyperlinkPhonebook":
                selectedLink = 3;
                break;
        }

        updateTable();

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

    @SuppressWarnings("unchecked")
    public void onAddButton() throws IOException {

        if (selectedLink == 0) {
            addEntityToTable("person", node -> {
            }, node -> ((TextField) node).getText());
        } else if (selectedLink == 1) {


            try (Statement statement = connection.createStatement()) {

                ResultSet resultSet = statement.executeQuery("select id, name from provider;");

                List<Pair<Integer, String>> providerRows = new ArrayList<>();

                while (resultSet.next()) {
                    providerRows.add(new Pair<>(resultSet.getInt(1), resultSet.getString(2)));
                }

                addEntityToTable("phone_number", form -> {
                    form.getChildrenUnmodifiable()
                            .stream()
                            .filter(node -> node instanceof VBox)
                            .map(node -> ((VBox) node).getChildren().get(1))
                            .filter(node -> node instanceof ChoiceBox)
                            .findFirst()
                            .map(node -> (ChoiceBox<Pair<Integer, String>>) node)
                            .ifPresent(choiceBox -> {
                                choiceBox.setConverter(new StringConverter<>() {
                                    @Override
                                    public String toString(Pair<Integer, String> object) {
                                        return object != null ? object.getValue() : "";
                                    }

                                    @Override
                                    public Pair<Integer, String> fromString(String string) {
                                        return null;
                                    }
                                });
                                choiceBox.getItems().addAll(providerRows);
                            });
                }, node -> {
                    if (node instanceof TextField) {
                        return ((TextField) node).getText();
                    }
                    return Integer.toString(((ChoiceBox<Pair<Integer, String>>) node).getSelectionModel().getSelectedItem().getKey());
                });
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else if (selectedLink == 2) {
            addEntityToTable("provider", node -> {
            }, node -> ((TextField) node).getText());
        }

    }

    private void addEntityToTable(final String tableName, Consumer<Parent> nodeContentSetter, Function<Node, String> nodeContentGetter) throws IOException {

        Parent content = FXMLLoader.load(getClass().getResource("/" + tableName + "-edit-form.fxml"));

        nodeContentSetter.accept(content);

        Dialog<List<String>> dialog = new Dialog<>();
        dialog.setTitle("Add/Edit " + tableName + " Dialog");
        dialog.setHeaderText("Creating a new row");

        // Set the button types.
        ButtonType loginButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);
        dialog.getDialogPane().setContent(content);

        // Convert the result to a username-password-pair when the login button is clicked.
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                return content.getChildrenUnmodifiable()
                        .stream()
                        .filter(node -> node instanceof VBox)
                        .map(node -> ((VBox) node).getChildren().get(1))
                        .map(nodeContentGetter)
                        .collect(Collectors.toList());
            }
            return null;
        });

        dialog.showAndWait().ifPresent(fields -> {

            String values = fields.stream()
                    .map(field -> "N'" + field + "'")
                    .collect(Collectors.joining(", ", "(", ")"));

            addRowAndShowTable("insert into " + tableName + " values " + values);
        });
    }

    private void addRowAndShowTable(String sqlQuery) {

        try (Statement statement = connection.createStatement()) {
            statement.execute(sqlQuery);
            updateTable();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateTable() throws SQLException {

        switch (selectedLink) {
            case 0:
                generateTable("select * from person;");
                break;
            case 1:
                generateTable("select phone_number.id as id, phone, type, name from phone_number join provider on provider.id = provider;");
                break;
            case 2:
                generateTable("select * from provider;");
                break;
            case 3:
                generateTable("select first_name, middle_name, last_name, phone, type, name from phone_contact \n" +
                        "\tjoin person on person.id = person_id\n" +
                        "\tjoin phone_number on phone_number.id = phone_number_id\n" +
                        "\tjoin provider on provider.id = phone_number.provider;");
        }
    }
}
