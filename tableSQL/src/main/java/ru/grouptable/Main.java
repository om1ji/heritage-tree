package ru.grouptable;

import ru.grouptable.dao.*;
import ru.grouptable.entity.*;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.table.TableColumnModel;
import javax.swing.RowSorter;
import javax.swing.RowSorter.SortKey;
import javax.swing.SortOrder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.Arrays;
import java.util.Comparator;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Main {
    private static SessionFactory sessionFactory;
    private static PersonDAO personDAO;
    private static DefaultTableModel personTableModel;
    private static FamilyTreePanel treePanel;
    private static String currentDatabasePath = "heritage.db";
    private static JFrame mainFrame;

    public static void main(String[] args) {
        initializeDatabase(currentDatabasePath);
        SwingUtilities.invokeLater(() -> createAndShowGUI());
    }

    private static void initializeDatabase(String dbPath) {
        if (sessionFactory != null) {
            sessionFactory.close();
        }

        Configuration config = new Configuration()
                .configure("hibernate.cfg.xml")
                .addAnnotatedClass(Person.class)
                .setProperty("hibernate.connection.url", "jdbc:sqlite:" + dbPath);
        
        sessionFactory = config.buildSessionFactory();
        personDAO = new PersonDAO(sessionFactory);
    }

    private static void createAndShowGUI() {
        mainFrame = new JFrame("Родовое древо");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setSize(1600, 900);

        // Create menu bar
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("Файл");
        
        JMenuItem newDbItem = new JMenuItem("Создать базу данных");
        JMenuItem openDbItem = new JMenuItem("Открыть базу данных");
        JMenuItem saveAsDbItem = new JMenuItem("Сохранить как...");
        
        newDbItem.addActionListener(e -> createNewDatabase());
        openDbItem.addActionListener(e -> openDatabase());
        saveAsDbItem.addActionListener(e -> saveAsDatabase());
        
        fileMenu.add(newDbItem);
        fileMenu.add(openDbItem);
        fileMenu.add(saveAsDbItem);
        menuBar.add(fileMenu);
        mainFrame.setJMenuBar(menuBar);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setLeftComponent(createPersonPanel());
        
        // Create right panel with tree controls
        treePanel = new FamilyTreePanel();
        JPanel rightPanel = new JPanel(new BorderLayout());
        
        // Tree controls panel
        JPanel treeControlsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JCheckBox showSiblingsCheckbox = new JCheckBox("Показать братьев/сестёр");
        JCheckBox showUnclesCheckbox = new JCheckBox("Показать дядей/тёть");
        
        showSiblingsCheckbox.addActionListener(e -> {
            treePanel.setShowSiblingsMode(showSiblingsCheckbox.isSelected());
        });
        
        showUnclesCheckbox.addActionListener(e -> {
            treePanel.setShowUnclesAndAunts(showUnclesCheckbox.isSelected());
        });
        
        treeControlsPanel.add(showSiblingsCheckbox);
        treeControlsPanel.add(showUnclesCheckbox);
        
        rightPanel.add(new JLabel("Выберите человека в таблице для отображения его родословной", 
            SwingConstants.CENTER), BorderLayout.NORTH);
        rightPanel.add(treeControlsPanel, BorderLayout.SOUTH);
        rightPanel.add(new JScrollPane(treePanel), BorderLayout.CENTER);
        
        splitPane.setRightComponent(rightPanel);
        splitPane.setDividerLocation(800);
        
        mainFrame.add(splitPane);
        mainFrame.setVisible(true);
    }

    private static void createNewDatabase() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Создать новую базу данных");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            public boolean accept(File f) {
                return f.getName().toLowerCase().endsWith(".db") || f.isDirectory();
            }
            public String getDescription() {
                return "SQLite Database (*.db)";
            }
        });

        int result = fileChooser.showSaveDialog(mainFrame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String filePath = selectedFile.getAbsolutePath();
            if (!filePath.toLowerCase().endsWith(".db")) {
                filePath += ".db";
            }
            
            // Delete existing file if it exists
            File dbFile = new File(filePath);
            if (dbFile.exists()) {
                dbFile.delete();
            }

            currentDatabasePath = filePath;
            initializeDatabase(currentDatabasePath);
            updatePersonTable(personTableModel);
            treePanel.setRootPerson(null);
            mainFrame.setTitle("Родовое древо - " + currentDatabasePath);
        }
    }

    private static void openDatabase() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Открыть базу данных");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            public boolean accept(File f) {
                return f.getName().toLowerCase().endsWith(".db") || f.isDirectory();
            }
            public String getDescription() {
                return "SQLite Database (*.db)";
            }
        });

        int result = fileChooser.showOpenDialog(mainFrame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            currentDatabasePath = selectedFile.getAbsolutePath();
            initializeDatabase(currentDatabasePath);
            updatePersonTable(personTableModel);
            treePanel.setRootPerson(null);
            mainFrame.setTitle("Родовое древо - " + currentDatabasePath);
        }
    }

    private static void saveAsDatabase() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Сохранить базу данных как");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            public boolean accept(File f) {
                return f.getName().toLowerCase().endsWith(".db") || f.isDirectory();
            }
            public String getDescription() {
                return "SQLite Database (*.db)";
            }
        });

        int result = fileChooser.showSaveDialog(mainFrame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String newPath = selectedFile.getAbsolutePath();
            if (!newPath.toLowerCase().endsWith(".db")) {
                newPath += ".db";
            }

            try {
                Files.copy(Paths.get(currentDatabasePath), Paths.get(newPath), 
                    StandardCopyOption.REPLACE_EXISTING);
                currentDatabasePath = newPath;
                initializeDatabase(currentDatabasePath);
                mainFrame.setTitle("Родовое древо - " + currentDatabasePath);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(mainFrame,
                    "Ошибка при сохранении базы данных: " + e.getMessage(),
                    "Ошибка",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static JPanel createPersonPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        String[] columns = {"ID", "Имя", "Фамилия", "Дата рождения", "Родители", "Братья/Сестры", "Дяди/Тёти"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        personTableModel = model;
        JTable table = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(table);

        // Добавляем сортировку по столбцам
        table.setAutoCreateRowSorter(true);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        // Сравнитель для сортировки дат в формате YYYY-MM-DD
        sorter.setComparator(3, new Comparator<String>() {
            @Override
            public int compare(String date1, String date2) {
                // Обработка пустых значений - они должны быть в конце
                if (date1 == null || date1.trim().isEmpty()) {
                    return (date2 == null || date2.trim().isEmpty()) ? 0 : 1;
                }
                if (date2 == null || date2.trim().isEmpty()) {
                    return -1;
                }

                try {
                    // Разбиваем даты на компоненты
                    String[] parts1 = date1.trim().split("-");
                    String[] parts2 = date2.trim().split("-");

                    // Сравниваем года
                    int year1 = Integer.parseInt(parts1[0]);
                    int year2 = Integer.parseInt(parts2[0]);
                    if (year1 != year2) {
                        return Integer.compare(year1, year2);
                    }

                    // Если года равны, сравниваем месяцы
                    int month1 = Integer.parseInt(parts1[1]);
                    int month2 = Integer.parseInt(parts2[1]);
                    if (month1 != month2) {
                        return Integer.compare(month1, month2);
                    }

                    // Если месяцы равны, сравниваем дни
                    int day1 = Integer.parseInt(parts1[2]);
                    int day2 = Integer.parseInt(parts2[2]);
                    return Integer.compare(day1, day2);

                } catch (Exception e) {
                    // В случае ошибки парсинга, отправляем некорректные значения в конец
                    return 1;
                }
            }
        });

        // Добавляем компараторы для остальных столбцов
        sorter.setComparator(0, new Comparator<Long>() {
            @Override
            public int compare(Long id1, Long id2) {
                return id1.compareTo(id2);
            }
        });

        // Компаратор для текстовых столбцов с учетом пустых значений
        Comparator<String> textComparator = new Comparator<String>() {
            @Override
            public int compare(String s1, String s2) {
                if (s1 == null || s1.trim().isEmpty()) {
                    return (s2 == null || s2.trim().isEmpty()) ? 0 : 1;
                }
                if (s2 == null || s2.trim().isEmpty()) {
                    return -1;
                }
                return s1.trim().compareToIgnoreCase(s2.trim());
            }
        };

        sorter.setComparator(1, textComparator); // Имя
        sorter.setComparator(2, textComparator); // Фамилия
        sorter.setComparator(4, textComparator); // Родители
        sorter.setComparator(5, textComparator); // Братья/Сестры
        sorter.setComparator(6, textComparator); // Дяди/Тёти

        // Добавляем обработчик кликов по заголовку для всех столбцов
        table.getTableHeader().addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int column = table.columnAtPoint(e.getPoint());
                List<? extends SortKey> sortKeys = sorter.getSortKeys();
                if (sortKeys.isEmpty() || sortKeys.get(0).getColumn() != column) {
                    sorter.setSortKeys(Arrays.asList(new RowSorter.SortKey(column, SortOrder.ASCENDING)));
                } else {
                    SortOrder currentOrder = sortKeys.get(0).getSortOrder();
                    SortOrder newOrder = currentOrder == SortOrder.ASCENDING ? 
                        SortOrder.DESCENDING : SortOrder.ASCENDING;
                    sorter.setSortKeys(Arrays.asList(new RowSorter.SortKey(column, newOrder)));
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {}
            @Override
            public void mouseReleased(MouseEvent e) {}
            @Override
            public void mouseEntered(MouseEvent e) {}
            @Override
            public void mouseExited(MouseEvent e) {}
        });

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = table.getSelectedRow();
                if (selectedRow != -1) {
                    Long id = (Long) table.getValueAt(selectedRow, 0);
                    Person selectedPerson = personDAO.findById(id);
                    treePanel.setRootPerson(selectedPerson);
                }
            }
        });

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        JButton addButton = new JButton("Добавить");
        JButton editButton = new JButton("Изменить");
        JButton deleteButton = new JButton("Удалить");
        JButton refreshButton = new JButton("Сбросить");
        JButton filterButton = new JButton("Фильтр");
        
        JTextField searchField = new JTextField(20);
        JButton searchButton = new JButton("Поиск");
        JButton clearSearchButton = new JButton("Очистить");

        controlPanel.add(addButton);
        controlPanel.add(editButton);
        controlPanel.add(deleteButton);
        controlPanel.add(refreshButton);
        controlPanel.add(filterButton);
        controlPanel.add(Box.createHorizontalStrut(20));
        controlPanel.add(searchField);
        controlPanel.add(searchButton);
        controlPanel.add(clearSearchButton);

        addButton.addActionListener(e -> showAddPersonDialog());
        editButton.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow != -1) {
                Long id = (Long) table.getValueAt(selectedRow, 0);
                showEditPersonDialog(personDAO.findById(id));
            }
        });
        deleteButton.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow != -1) {
                Long id = (Long) table.getValueAt(selectedRow, 0);
                int confirm = JOptionPane.showConfirmDialog(
                    panel,
                    "Вы уверены, что хотите удалить этого человека?",
                    "Подтверждение удаления",
                    JOptionPane.YES_NO_OPTION
                );
                if (confirm == JOptionPane.YES_OPTION) {
                    personDAO.delete(personDAO.findById(id));
                    updatePersonTable(model);
                    treePanel.setRootPerson(null);
                }
            }
        });
        
        ActionListener searchAction = e -> {
            String searchText = searchField.getText().trim();
            if (!searchText.isEmpty()) {
                List<Person> searchResults = personDAO.findByNameLike(searchText);
                updatePersonTable(model, searchResults);
                if (searchResults.isEmpty()) {
                    JOptionPane.showMessageDialog(
                        panel,
                        "Никого не найдено",
                        "Результаты поиска",
                        JOptionPane.INFORMATION_MESSAGE
                    );
                }
            }
        };
        
        searchButton.addActionListener(searchAction);
        searchField.addActionListener(searchAction);
        
        clearSearchButton.addActionListener(e -> {
            searchField.setText("");
            updatePersonTable(model);
        });
        
        refreshButton.addActionListener(e -> updatePersonTable(model));

        filterButton.addActionListener(e -> showFilterDialog(model));

        searchField.getInputMap().put(KeyStroke.getKeyStroke("ESCAPE"), "clear");
        searchField.getActionMap().put("clear", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchField.setText("");
                updatePersonTable(model);
            }
        });

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(controlPanel, BorderLayout.CENTER);

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        updatePersonTable(model);

        return panel;
    }

    private static void updatePersonTable(DefaultTableModel model) {
        updatePersonTable(model, personDAO.getAllPersons());
    }

    private static void updatePersonTable(DefaultTableModel model, List<Person> persons) {
        model.setRowCount(0);
        for (Person person : persons) {
            String parentNames = person.getParents().stream()
                .map(parent -> parent.getFirstName() + " " + parent.getLastName())
                .collect(Collectors.joining(", "));

            String siblingNames = person.getSiblings().stream()
                .map(sibling -> sibling.getFirstName() + " " + sibling.getLastName())
                .collect(Collectors.joining(", "));

            String uncleAuntNames = person.getUnclesAndAunts().stream()
                .map(relative -> relative.getFirstName() + " " + relative.getLastName())
                .collect(Collectors.joining(", "));

            model.addRow(new Object[]{
                person.getId(),
                person.getFirstName(),
                person.getLastName(),
                person.getBirthDate(),
                parentNames,
                siblingNames,
                uncleAuntNames
            });
        }
    }

    private static void showAddPersonDialog() {
        JDialog dialog = new JDialog();
        dialog.setTitle("Добавить");
        dialog.setModal(true);
        dialog.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        JTextField firstNameField = new JTextField(20);
        JTextField lastNameField = new JTextField(20);
        JTextField birthDateField = new JTextField(20);

        // Add input validation for names
        firstNameField.setInputVerifier(new InputVerifier() {
            @Override
            public boolean verify(JComponent input) {
                String text = ((JTextField) input).getText();
                if (!text.matches("[а-яА-ЯёЁa-zA-Z\\s-]+")) {
                    JOptionPane.showMessageDialog(dialog,
                        "Имя может содержать только буквы, пробелы и дефис",
                        "Ошибка ввода",
                        JOptionPane.ERROR_MESSAGE);
                    return false;
                }
                return true;
            }
        });

        lastNameField.setInputVerifier(new InputVerifier() {
            @Override
            public boolean verify(JComponent input) {
                String text = ((JTextField) input).getText();
                if (!text.matches("[а-яА-ЯёЁa-zA-Z\\s-]+")) {
                    JOptionPane.showMessageDialog(dialog,
                        "Фамилия может содержать только буквы, пробелы и дефис",
                        "Ошибка ввода",
                        JOptionPane.ERROR_MESSAGE);
                    return false;
                }
                return true;
            }
        });

        // Add date format validation
        birthDateField.setInputVerifier(new InputVerifier() {
            @Override
            public boolean verify(JComponent input) {
                String text = ((JTextField) input).getText();
                if (!text.matches("\\d{2}\\-\\d{2}\\-\\d{4}")) {
                    JOptionPane.showMessageDialog(dialog,
                        "Дата должна быть в формате ГГГГ-ММ-ДД",
                        "Ошибка ввода",
                        JOptionPane.ERROR_MESSAGE);
                    return false;
                }
                // Validate date values
                try {
                    String[] parts = text.split("\\.");
                    int day = Integer.parseInt(parts[0]);
                    int month = Integer.parseInt(parts[1]);
                    int year = Integer.parseInt(parts[2]);
                    
                    if (day < 1 || day > 31 || month < 1 || month > 12 || 
                        year < 1900 || year > 2100) {
                        throw new NumberFormatException();
                    }
                    
                    // Check days in month
                    if (month == 2) {
                        boolean isLeap = (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
                        if (day > (isLeap ? 29 : 28)) {
                            throw new NumberFormatException();
                        }
                    } else if ((month == 4 || month == 6 || month == 9 || month == 11) && day > 30) {
                        throw new NumberFormatException();
                    }
                    
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(dialog,
                        "Введите корректную дату",
                        "Ошибка ввода",
                        JOptionPane.ERROR_MESSAGE);
                    return false;
                }
                return true;
            }
        });

        JList<Person> parentList = new JList<>(
            personDAO.getAllPersons().toArray(new Person[0])
        );
        parentList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        
        // Add parent selection limit
        parentList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && parentList.getSelectedIndices().length > 2) {
                JOptionPane.showMessageDialog(dialog,
                    "Нельзя выбрать больше двух родителей",
                    "Ошибка выбора",
                    JOptionPane.ERROR_MESSAGE);
                parentList.setSelectedIndices(Arrays.copyOf(parentList.getSelectedIndices(), 2));
            }
        });
        
        JScrollPane parentScrollPane = new JScrollPane(parentList);
        parentScrollPane.setPreferredSize(new Dimension(200, 100));

        gbc.gridx = 0; gbc.gridy = 0;
        dialog.add(new JLabel("Имя:"), gbc);
        gbc.gridx = 1;
        dialog.add(firstNameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        dialog.add(new JLabel("Фамилия:"), gbc);
        gbc.gridx = 1;
        dialog.add(lastNameField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        dialog.add(new JLabel("Дата рождения (ДД.ММ.ГГГГ):"), gbc);
        gbc.gridx = 1;
        dialog.add(birthDateField, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        dialog.add(new JLabel("Родители (макс. 2):"), gbc);
        gbc.gridx = 1;
        dialog.add(parentScrollPane, gbc);

        JButton saveButton = new JButton("Сохранить");
        gbc.gridx = 0; gbc.gridy = 4;
        gbc.gridwidth = 2;
        dialog.add(saveButton, gbc);

        saveButton.addActionListener(e -> {
            // Validate all fields before saving
            if (!firstNameField.getInputVerifier().verify(firstNameField) ||
                !lastNameField.getInputVerifier().verify(lastNameField) ||
                !birthDateField.getInputVerifier().verify(birthDateField)) {
                return;
            }
            
            if (firstNameField.getText().trim().isEmpty() || 
                lastNameField.getText().trim().isEmpty() || 
                birthDateField.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(dialog,
                    "Все поля должны быть заполнены",
                    "Ошибка ввода",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            Person person = new Person();
            person.setFirstName(firstNameField.getText().trim());
            person.setLastName(lastNameField.getText().trim());
            person.setBirthDate(birthDateField.getText().trim());
            person.setParents(new HashSet<>(parentList.getSelectedValuesList()));
            
            personDAO.save(person);
            updatePersonTable(personTableModel);
            dialog.dispose();
        });

        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    private static void showEditPersonDialog(Person person) {
        JDialog dialog = new JDialog();
        dialog.setTitle("Изменить");
        dialog.setModal(true);
        dialog.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        JTextField firstNameField = new JTextField(person.getFirstName(), 20);
        JTextField lastNameField = new JTextField(person.getLastName(), 20);
        JTextField birthDateField = new JTextField(person.getBirthDate(), 20);

        // Add input validation for names
        firstNameField.setInputVerifier(new InputVerifier() {
            @Override
            public boolean verify(JComponent input) {
                String text = ((JTextField) input).getText();
                if (!text.matches("[а-яА-ЯёЁa-zA-Z\\s-]+")) {
                    JOptionPane.showMessageDialog(dialog,
                        "Имя может содержать только буквы, пробелы и дефис",
                        "Ошибка ввода",
                        JOptionPane.ERROR_MESSAGE);
                    return false;
                }
                return true;
            }
        });

        lastNameField.setInputVerifier(new InputVerifier() {
            @Override
            public boolean verify(JComponent input) {
                String text = ((JTextField) input).getText();
                if (!text.matches("[а-яА-ЯёЁa-zA-Z\\s-]+")) {
                    JOptionPane.showMessageDialog(dialog,
                        "Фамилия может содержать только буквы, пробелы и дефис",
                        "Ошибка ввода",
                        JOptionPane.ERROR_MESSAGE);
                    return false;
                }
                return true;
            }
        });

        // Add date format validation
        birthDateField.setInputVerifier(new InputVerifier() {
            @Override
            public boolean verify(JComponent input) {
                String text = ((JTextField) input).getText();
                if (!text.matches("\\d{2}\\.\\d{2}\\.\\d{4}")) {
                    JOptionPane.showMessageDialog(dialog,
                        "Дата должна быть в формате ДД.ММ.ГГГГ",
                        "Ошибка ввода",
                        JOptionPane.ERROR_MESSAGE);
                    return false;
                }
                // Validate date values
                try {
                    String[] parts = text.split("\\.");
                    int day = Integer.parseInt(parts[0]);
                    int month = Integer.parseInt(parts[1]);
                    int year = Integer.parseInt(parts[2]);
                    
                    if (day < 1 || day > 31 || month < 1 || month > 12 || 
                        year < 1900 || year > 2100) {
                        throw new NumberFormatException();
                    }
                    
                    // Check days in month
                    if (month == 2) {
                        boolean isLeap = (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
                        if (day > (isLeap ? 29 : 28)) {
                            throw new NumberFormatException();
                        }
                    } else if ((month == 4 || month == 6 || month == 9 || month == 11) && day > 30) {
                        throw new NumberFormatException();
                    }
                    
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(dialog,
                        "Введите корректную дату",
                        "Ошибка ввода",
                        JOptionPane.ERROR_MESSAGE);
                    return false;
                }
                return true;
            }
        });

        JList<Person> parentList = new JList<>(
            personDAO.getAllPersons().toArray(new Person[0])
        );
        parentList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        
        // Add parent selection limit
        parentList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && parentList.getSelectedIndices().length > 2) {
                JOptionPane.showMessageDialog(dialog,
                    "Нельзя выбрать больше двух родителей",
                    "Ошибка выбора",
                    JOptionPane.ERROR_MESSAGE);
                parentList.setSelectedIndices(Arrays.copyOf(parentList.getSelectedIndices(), 2));
            }
        });
        
        List<Person> allPersons = personDAO.getAllPersons();
        int[] selectedIndices = person.getParents().stream()
            .mapToInt(allPersons::indexOf)
            .toArray();
        parentList.setSelectedIndices(selectedIndices);
        
        JScrollPane parentScrollPane = new JScrollPane(parentList);
        parentScrollPane.setPreferredSize(new Dimension(200, 100));

        gbc.gridx = 0; gbc.gridy = 0;
        dialog.add(new JLabel("Имя:"), gbc);
        gbc.gridx = 1;
        dialog.add(firstNameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        dialog.add(new JLabel("Фамилия:"), gbc);
        gbc.gridx = 1;
        dialog.add(lastNameField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        dialog.add(new JLabel("Дата рождения (ДД.ММ.ГГГГ):"), gbc);
        gbc.gridx = 1;
        dialog.add(birthDateField, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        dialog.add(new JLabel("Родители (макс. 2):"), gbc);
        gbc.gridx = 1;
        dialog.add(parentScrollPane, gbc);

        JButton saveButton = new JButton("Сохранить");
        gbc.gridx = 0; gbc.gridy = 4;
        gbc.gridwidth = 2;
        dialog.add(saveButton, gbc);

        saveButton.addActionListener(e -> {
            // Validate all fields before saving
            if (!firstNameField.getInputVerifier().verify(firstNameField) ||
                !lastNameField.getInputVerifier().verify(lastNameField) ||
                !birthDateField.getInputVerifier().verify(birthDateField)) {
                return;
            }
            
            if (firstNameField.getText().trim().isEmpty() || 
                lastNameField.getText().trim().isEmpty() || 
                birthDateField.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(dialog,
                    "Все поля должны быть заполнены",
                    "Ошибка ввода",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            person.setFirstName(firstNameField.getText().trim());
            person.setLastName(lastNameField.getText().trim());
            person.setBirthDate(birthDateField.getText().trim());
            person.setParents(new HashSet<>(parentList.getSelectedValuesList()));
            
            personDAO.save(person);
            updatePersonTable(personTableModel);
            dialog.dispose();
        });

        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    private static void showFilterDialog(DefaultTableModel model) {
        JDialog dialog = new JDialog();
        dialog.setTitle("Расширенный фильтр");
        dialog.setModal(true);
        dialog.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Name filter
        JPanel namePanel = new JPanel(new BorderLayout(5, 0));
        namePanel.setBorder(BorderFactory.createTitledBorder("Поиск по имени"));
        JTextField nameField = new JTextField(20);
        JLabel nameHint = new JLabel("Введите начало имени");
        nameHint.setFont(nameHint.getFont().deriveFont(Font.ITALIC));
        nameHint.setForeground(Color.GRAY);
        
        JPanel nameInputPanel = new JPanel(new BorderLayout());
        nameInputPanel.add(nameField, BorderLayout.CENTER);
        nameInputPanel.add(nameHint, BorderLayout.SOUTH);
        namePanel.add(nameInputPanel);

        // Birth date filter
        JPanel datePanel = new JPanel(new GridBagLayout());
        datePanel.setBorder(BorderFactory.createTitledBorder("Дата рождения"));
        GridBagConstraints dateGbc = new GridBagConstraints();
        dateGbc.insets = new Insets(2, 2, 2, 2);
        
        dateGbc.gridx = 0; dateGbc.gridy = 0;
        datePanel.add(new JLabel("С:"), dateGbc);
        dateGbc.gridx = 1;
        JTextField fromDateField = new JTextField(10);
        datePanel.add(fromDateField, dateGbc);
        
        dateGbc.gridx = 2;
        datePanel.add(new JLabel("По:"), dateGbc);
        dateGbc.gridx = 3;
        JTextField toDateField = new JTextField(10);
        datePanel.add(toDateField, dateGbc);

        // Add components to dialog
        gbc.gridx = 0; gbc.gridy = 0;
        dialog.add(namePanel, gbc);

        gbc.gridy = 1;
        dialog.add(datePanel, gbc);

        // Buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton applyButton = new JButton("Применить");
        JButton resetButton = new JButton("Сбросить");
        buttonPanel.add(applyButton);
        buttonPanel.add(resetButton);

        gbc.gridy = 2;
        dialog.add(buttonPanel, gbc);

        // Action listeners
        applyButton.addActionListener(e -> {
            String nameFilter = nameField.getText().trim();
            String fromDate = fromDateField.getText().trim();
            String toDate = toDateField.getText().trim();
            
            List<Person> filteredPersons = personDAO.findByFilters(nameFilter, fromDate, toDate);
            updatePersonTable(model, filteredPersons);
            dialog.dispose();
            
            if (filteredPersons.isEmpty()) {
                JOptionPane.showMessageDialog(
                    dialog,
                    "Никого не найдено",
                    "Результаты фильтрации",
                    JOptionPane.INFORMATION_MESSAGE
                );
            }
        });

        resetButton.addActionListener(e -> {
            nameField.setText("");
            fromDateField.setText("");
            toDateField.setText("");
            updatePersonTable(model);
        });

        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }
}
