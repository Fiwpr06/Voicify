package com.voicify.controller;

import com.voicify.model.TranslationHistory;
import com.voicify.model.TranslationHistoryDAO;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.VBox;
import java.util.List;
import java.util.Optional;

public class HistoryController extends BaseController {
    @FXML private VBox root;
    @FXML private TableView<TranslationHistory> historyTableView;
    @FXML private TableColumn<TranslationHistory, Integer> idColumn;
    @FXML private TableColumn<TranslationHistory, String> sourceColumn;
    @FXML private TableColumn<TranslationHistory, String> translationColumn;
    @FXML private TableColumn<TranslationHistory, String> typeColumn;
    @FXML private TableColumn<TranslationHistory, String> dateColumn;
    @FXML private TextField searchField;
    @FXML private Button searchButton;
    @FXML private Button exportButton;
    @FXML private Button deleteButton;
    @FXML private Button refreshButton;

    private ObservableList<TranslationHistory> historyData = FXCollections.observableArrayList();
    private TranslationHistoryDAO historyDAO = new TranslationHistoryDAO();

    @FXML
    private void handleSearchButton() {
        // Search implementation
    }

    @FXML
    private void handleExportButton() {
        // Export implementation
    }

    @FXML
    private void handleDeleteButton() {
        TranslationHistory selected = historyTableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert confirmDialog = new Alert(AlertType.CONFIRMATION);
            confirmDialog.setTitle("Confirm Delete");
            confirmDialog.setHeaderText("Delete Translation Record");
            confirmDialog.setContentText("Are you sure you want to delete this translation record?");

            Optional<ButtonType> result = confirmDialog.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                try {
                    historyDAO.delete(selected.getId());
                    historyData.remove(selected);
                    System.out.println("Translation history deleted successfully");
                } catch (Exception e) {
                    System.out.println("Failed to delete translation history: " + e.getMessage());
                    showAlert(AlertType.ERROR, "Delete Error",
                        "Failed to delete translation history: " + e.getMessage());
                }
            }
        } else {
            showAlert(AlertType.WARNING, "No Selection",
                "Please select a translation record to delete.");
        }
    }

    @FXML
    private void handleRefreshButton() {
        // Refresh implementation
    }

    private void loadHistoryData() {
        try {
            List<TranslationHistory> history = historyDAO.findAll();
            historyData.setAll(history);
            historyTableView.setItems(historyData);
        } catch (Exception e) {
            showAlert(AlertType.ERROR, "Load Error",
                "Failed to load translation history: " + e.getMessage());
        }
    }

    private void showAlert(AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @Override
    protected Object getRootNode() {
        return root;
    }
}