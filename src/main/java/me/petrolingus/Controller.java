package me.petrolingus;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.util.Properties;

public class Controller {

    public TableView<ObservableList<String>> table;

    public TextField textField;

    private Connection connection;

    private Properties columnMappings;

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

    public void onKeyTyped() {

    }

    public void onLinkPressed(ActionEvent event) throws SQLException {
        Hyperlink hyperlink = ((Hyperlink) event.getSource());
        switch (hyperlink.getId()) {
            case "hyperlinkPeople":
                generateTable("select * from person;");
                break;
            case "hyperlinkPhones":
                generateTable("select * from phone_number join provider on provider.id = provider;");
                break;
            case "hyperlinkProviders":
                generateTable("select * from provider;");
                break;
            case "hyperlinkPhonebook":
                generateTable("select first_name, middle_name, last_name, phone, type, name from phone_contact \n" +
                        "\tjoin person on person.id = person_id\n" +
                        "\tjoin phone_number on phone_number.id = phone_number_id\n" +
                        "\tjoin provider on provider.id = phone_number.provider;");
                break;
            default:
        }
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

}
