package com.battleship.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class UsernameInputUI extends JDialog {

    private JTextField usernameField;
    private String enteredUsername = null; 

    public UsernameInputUI(JFrame parent) {
        
        super(parent, "Kullanıcı Adı Girin", true);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5); 

        JLabel label = new JLabel("Kullanıcı Adınız:");
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(label, gbc);

        usernameField = new JTextField(20); 
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL; 
        gbc.weightx = 1.0; 
        panel.add(usernameField, gbc);

        JButton okButton = new JButton("Devam Et");
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2; 
        gbc.anchor = GridBagConstraints.CENTER; 
        gbc.fill = GridBagConstraints.NONE; 
        panel.add(okButton, gbc);

        
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String input = usernameField.getText().trim();
                if (!input.isEmpty()) {
                    enteredUsername = input; 
                    dispose(); 
                } else {
                    JOptionPane.showMessageDialog(UsernameInputUI.this, "Lütfen bir kullanıcı adı girin.", "Uyarı", JOptionPane.WARNING_MESSAGE);
                    
                }
            }
        });

        
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        setContentPane(panel);
        pack(); 
        setLocationRelativeTo(parent); 
    }

    
    public String getEnteredUsername() {
        return enteredUsername;
    }

    
     @Override
     public void addNotify() {
         super.addNotify();
         usernameField.requestFocusInWindow();
     }
}