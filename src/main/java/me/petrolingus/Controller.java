package me.petrolingus;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
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

        generateTable();
    }

    public void onKeyTyped() throws SQLException {
        generateTable();
    }

    public void onLinkPressed(ActionEvent event) {
        Hyperlink hyperlink = ((Hyperlink) event.getSource());
//        System.out.println(hyperlink.);
    }

    private void generateTable() throws SQLException {

        Statement statement = connection.createStatement();
        ResultSet resultSet;

        String str = textField.getText();
        if (str.length() > 0) {
            resultSet = statement.executeQuery("SELECT first_name, middle_name, last_name FROM person WHERE last_name LIKE N'%" + str + "%';");
        } else {
            resultSet = statement.executeQuery("SELECT first_name, middle_name, last_name FROM person;");
        }

        table.getColumns().clear();
        for (int i = 0; i < resultSet.getMetaData().getColumnCount(); i++) {
            final int j = i;
            String dbColumnName = resultSet.getMetaData().getColumnName(i + 1);
            String columnName = columnMappings.getProperty(dbColumnName, dbColumnName);
            TableColumn<ObservableList<String>, String> column = new TableColumn<>(columnName);
            column.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().get(j)));
            table.getColumns().add(column);
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
