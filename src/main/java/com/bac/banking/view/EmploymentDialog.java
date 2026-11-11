package com.bac.banking.view;

import com.bac.banking.controller.CustomerController;
import com.bac.banking.controller.dto.CustomerProfile;
import com.bac.banking.exception.BankingException;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;

/** Boundary class for the Update Employment use case (required before opening a Cheque Account). */
public class EmploymentDialog extends Dialog<Boolean> {

    public EmploymentDialog(CustomerController controller, Window owner, CustomerProfile profile) {
        initOwner(owner);
        setTitle("Employment details");
        setHeaderText("Where do you work?");

        TextField company = new TextField(profile.companyName());
        TextField address = new TextField(profile.companyAddress());
        TextField title = new TextField(profile.jobTitle());
        Label error = UiHelper.errorLabel();
        error.setMaxWidth(340);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Company name"), 0, 0);
        grid.add(company, 1, 0);
        grid.add(new Label("Company address"), 0, 1);
        grid.add(address, 1, 1);
        grid.add(new Label("Job title"), 0, 2);
        grid.add(title, 1, 2);
        grid.add(error, 0, 3, 2, 1);
        getDialogPane().setContent(grid);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Node okButton = getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            try {
                controller.updateEmployment(company.getText(), address.getText(), title.getText());
            } catch (BankingException ex) {
                error.setText(ex.getMessage());
                event.consume();
            }
        });
        setResultConverter(button -> button == ButtonType.OK);
    }
}
