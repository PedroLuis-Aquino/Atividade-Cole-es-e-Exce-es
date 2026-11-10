import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/** Simple Swing application for registering and listing patients. */
public class Main {
    private static final Path DATA_FILE = Paths.get("pacientes.txt");
    private final List<Patient> patients = new ArrayList<>();
    private final JFrame frame = new JFrame("Cadastro de Pacientes");
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel screens = new JPanel(cardLayout);
    private final JTextField nameField = new JTextField(24);
    private final JTextField ageField = new JTextField(24);
    private final JTextField phoneField = new JTextField(24);
    private final JTextField diagnosisField = new JTextField(24);
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"Nome", "Idade", "Telefone", "Diagnóstico"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private Main() {
        loadPatients();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(620, 400));
        frame.setSize(760, 500);
        frame.setLocationRelativeTo(null);

        JPanel navigation = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addScreenButton = new JButton("Adicionar paciente");
        JButton listScreenButton = new JButton("Listar pacientes");
        addScreenButton.addActionListener(event -> showScreen("add"));
        listScreenButton.addActionListener(event -> {
            refreshTable();
            showScreen("list");
        });
        navigation.add(addScreenButton);
        navigation.add(listScreenButton);

        screens.add(createAddScreen(), "add");
        screens.add(createListScreen(), "list");
        frame.add(navigation, BorderLayout.NORTH);
        frame.add(screens, BorderLayout.CENTER);
        frame.setVisible(true);
        showScreen("list");
    }

    private JPanel createAddScreen() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(8, 8, 8, 8);
        constraints.anchor = GridBagConstraints.WEST;

        addFormRow(panel, constraints, 0, "Nome:", nameField);
        addFormRow(panel, constraints, 1, "Idade:", ageField);
        addFormRow(panel, constraints, 2, "Telefone:", phoneField);
        addFormRow(panel, constraints, 3, "Diagnóstico:", diagnosisField);

        JButton saveButton = new JButton("Salvar paciente");
        saveButton.addActionListener(event -> savePatientFromForm());
        constraints.gridx = 1;
        constraints.gridy = 4;
        constraints.anchor = GridBagConstraints.CENTER;
        panel.add(saveButton, constraints);
        return panel;
    }

    private void addFormRow(JPanel panel, GridBagConstraints constraints, int row,
                            String label, JTextField field) {
        constraints.gridy = row;
        constraints.gridx = 0;
        constraints.weightx = 0;
        constraints.fill = GridBagConstraints.NONE;
        panel.add(new JLabel(label), constraints);
        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        panel.add(field, constraints);
    }

    private JPanel createListScreen() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JTable table = new JTable(tableModel);
        table.setFillsViewportHeight(true);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        JLabel hint = new JLabel("Pacientes cadastrados: os dados são salvos em pacientes.txt.");
        hint.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
        panel.add(hint, BorderLayout.SOUTH);
        return panel;
    }

    private void savePatientFromForm() {
        try {
            String name = nameField.getText().trim();
            String ageText = ageField.getText().trim();
            int age;
            try {
                age = Integer.parseInt(ageText);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Informe uma idade válida, usando apenas números.");
            }

            Patient patient = new Patient(name, age, phoneField.getText().trim(),
                    diagnosisField.getText().trim());
            patients.add(patient);
            try {
                persistPatients();
            } catch (Exception exception) {
                patients.remove(patients.size() - 1);
                throw exception;
            }

            clearForm();
            refreshTable();
            showScreen("list");
            JOptionPane.showMessageDialog(frame, "Paciente cadastrado com sucesso.",
                    "Cadastro concluído", JOptionPane.INFORMATION_MESSAGE);
        } catch (IllegalArgumentException exception) {
            showError(exception.getMessage());
        } catch (Exception exception) {
            showError("Não foi possível salvar o paciente. Verifique as permissões e o espaço em disco.\n"
                    + safeMessage(exception));
        }
    }

    private void loadPatients() {
        if (!Files.exists(DATA_FILE)) {
            return;
        }
        int invalidLines = 0;
        try {
            List<String> lines = Files.readAllLines(DATA_FILE, StandardCharsets.UTF_8);
            for (String line : lines) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                try {
                    String[] fields = line.split(",", -1);
                    if (fields.length != 4) {
                        throw new IllegalArgumentException("Registro incompleto.");
                    }
                    Base64.Decoder decoder = Base64.getUrlDecoder();
                    String name = decode(decoder, fields[0]);
                    int age = Integer.parseInt(decode(decoder, fields[1]));
                    String phone = decode(decoder, fields[2]);
                    String diagnosis = decode(decoder, fields[3]);
                    patients.add(new Patient(name, age, phone, diagnosis));
                } catch (Exception exception) {
                    invalidLines++;
                }
            }
        } catch (Exception exception) {
            SwingUtilities.invokeLater(() -> showError(
                    "Não foi possível ler o arquivo de pacientes. O programa continuará funcionando.\n"
                            + safeMessage(exception)));
        }
        if (invalidLines > 0) {
            final int count = invalidLines;
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(frame,
                    count + " registro(s) inválido(s) foram ignorados ao abrir o arquivo.",
                    "Aviso ao carregar dados", JOptionPane.WARNING_MESSAGE));
        }
    }

    private void persistPatients() throws IOException {
        List<String> lines = new ArrayList<>();
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        for (Patient patient : patients) {
            lines.add(encode(encoder, patient.name) + ","
                    + encode(encoder, Integer.toString(patient.age)) + ","
                    + encode(encoder, patient.phone) + ","
                    + encode(encoder, patient.diagnosis));
        }

        Path absoluteFile = DATA_FILE.toAbsolutePath();
        Path parent = absoluteFile.getParent();
        Path temporaryFile = Files.createTempFile(parent, "pacientes-", ".tmp");
        try {
            Files.write(temporaryFile, lines, StandardCharsets.UTF_8);
            try {
                Files.move(temporaryFile, absoluteFile, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporaryFile, absoluteFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    private static String encode(Base64.Encoder encoder, String value) {
        return encoder.encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(Base64.Decoder decoder, String value) {
        return new String(decoder.decode(value), StandardCharsets.UTF_8);
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (Patient patient : patients) {
            tableModel.addRow(new Object[]{patient.name, patient.age, patient.phone, patient.diagnosis});
        }
    }

    private void clearForm() {
        nameField.setText("");
        ageField.setText("");
        phoneField.setText("");
        diagnosisField.setText("");
    }

    private void showScreen(String screen) {
        cardLayout.show(screens, screen);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(frame, message, "Erro", JOptionPane.ERROR_MESSAGE);
    }

    private static String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.trim().isEmpty() ? exception.getClass().getSimpleName() : message;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new Main();
            } catch (Exception exception) {
                JOptionPane.showMessageDialog(null,
                        "O programa encontrou um erro ao iniciar, mas foi encerrado de forma segura.\n"
                                + safeMessage(exception),
                        "Erro de inicialização", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private static final class Patient {
        private final String name;
        private final int age;
        private final String phone;
        private final String diagnosis;

        private Patient(String name, int age, String phone, String diagnosis) {
            if (name == null || name.trim().isEmpty() || name.length() > 100) {
                throw new IllegalArgumentException("O nome é obrigatório e deve ter até 100 caracteres.");
            }
            if (age < 0 || age > 130) {
                throw new IllegalArgumentException("A idade deve estar entre 0 e 130 anos.");
            }
            if (phone == null || phone.trim().isEmpty() || phone.length() > 30
                    || !phone.matches("[0-9+() .-]+") || phone.replaceAll("\\D", "").length() < 7
                    || phone.replaceAll("\\D", "").length() > 20) {
                throw new IllegalArgumentException("Informe um telefone válido (de 7 a 20 dígitos).");
            }
            if (diagnosis == null || diagnosis.trim().isEmpty() || diagnosis.length() > 200) {
                throw new IllegalArgumentException("O diagnóstico é obrigatório e deve ter até 200 caracteres.");
            }
            this.name = name;
            this.age = age;
            this.phone = phone;
            this.diagnosis = diagnosis;
        }
    }
}
