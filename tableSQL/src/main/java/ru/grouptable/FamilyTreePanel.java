package ru.grouptable;

import ru.grouptable.entity.Person;
import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class FamilyTreePanel extends JPanel {
    private Person rootPerson;
    private static final int PERSON_WIDTH = 150;
    private static final int PERSON_HEIGHT = 60;
    private static final int VERTICAL_GAP = 50;
    private static final int HORIZONTAL_GAP = 40;
    private Map<Person, Point> personPositions;
    private Map<Integer, Set<Integer>> levelPositions;
    private boolean showSiblingsMode = false;
    private boolean showUnclesAndAunts = false;
    private int minX = 0;
    private int maxX = 0;
    private int minY = 0;
    private int maxY = 0;

    public FamilyTreePanel() {
        setPreferredSize(new Dimension(800, 600));
        personPositions = new HashMap<>();
        levelPositions = new HashMap<>();
    }

    public void setRootPerson(Person person) {
        this.rootPerson = person;
        recalculateTree();
    }

    public void setShowSiblingsMode(boolean showSiblingsMode) {
        this.showSiblingsMode = showSiblingsMode;
        recalculateTree();
    }

    public void setShowUnclesAndAunts(boolean show) {
        this.showUnclesAndAunts = show;
        recalculateTree();
    }

    private void recalculateTree() {
        personPositions.clear();
        levelPositions.clear();
        
        maxX = 0;
        maxY = 0;
        
        minY = getHeight() - PERSON_HEIGHT - 100;
        minX = getWidth() / 2 - PERSON_WIDTH / 2;

        if (rootPerson != null) {
            calculatePositions(0, 0);
            
            int width = maxX - minX + PERSON_WIDTH + 100;
            int height = maxY - minY + PERSON_HEIGHT + 100;
            
            int shiftX = Math.abs(minX) + 50;
            int shiftY = Math.abs(minY) + 50;
            
            Map<Person, Point> newPositions = new HashMap<>();
            for (Map.Entry<Person, Point> entry : personPositions.entrySet()) {
                Point oldPoint = entry.getValue();
                newPositions.put(entry.getKey(), new Point(oldPoint.x + shiftX, oldPoint.y + shiftY));
            }
            personPositions = newPositions;
            
            setPreferredSize(new Dimension(width, height));
        }
        
        revalidate();
        repaint();
    }

    private void calculatePositions(int startX, int startY) {
        if (showSiblingsMode) {
            calculateSiblingsView(startX, startY);
        } else {
            calculateAncestorsView(startX, startY);
        }
    }

    private void updateBounds(int x, int y) {
        minX = Math.min(minX, x);
        maxX = Math.max(maxX, x);
        minY = Math.min(minY, y);
        maxY = Math.max(maxY, y);
    }

    private void calculateSiblingsView(int startX, int startY) {
        personPositions.put(rootPerson, new Point(startX, startY));
        updateBounds(startX, startY);

        Set<Person> siblings = new HashSet<>();
        Set<Person> rootParents = rootPerson.getParents();
        
        if (!rootParents.isEmpty()) {
            for (Person parent : rootParents) {
                siblings.addAll(parent.getChildren());
            }
            siblings.remove(rootPerson);

            if (!siblings.isEmpty()) {
                List<Person> sortedSiblings = new ArrayList<>(siblings);
                sortedSiblings.sort(Comparator.comparing(Person::getId));

                int leftCount = sortedSiblings.size() / 2;
                int rightCount = sortedSiblings.size() - leftCount;
                
                for (int i = 0; i < leftCount; i++) {
                    int siblingX = startX - (leftCount - i) * (PERSON_WIDTH + HORIZONTAL_GAP);
                    personPositions.put(sortedSiblings.get(i), new Point(siblingX, startY));
                    updateBounds(siblingX, startY);
                }
                
                for (int i = 0; i < rightCount; i++) {
                    int siblingX = startX + (i + 1) * (PERSON_WIDTH + HORIZONTAL_GAP);
                    personPositions.put(sortedSiblings.get(leftCount + i), new Point(siblingX, startY));
                    updateBounds(siblingX, startY);
                }
            }

            int parentY = startY - PERSON_HEIGHT - VERTICAL_GAP;
            List<Person> sortedParents = new ArrayList<>(rootParents);
            sortedParents.sort(Comparator.comparing(Person::getId));

            int totalParentWidth = sortedParents.size() * PERSON_WIDTH + (sortedParents.size() - 1) * HORIZONTAL_GAP;
            int parentStartX = startX - totalParentWidth / 2;

            for (int i = 0; i < sortedParents.size(); i++) {
                Person parent = sortedParents.get(i);
                int parentX = parentStartX + i * (PERSON_WIDTH + HORIZONTAL_GAP);
                personPositions.put(parent, new Point(parentX, parentY));
                updateBounds(parentX, parentY);

                if (showUnclesAndAunts) {
                    addUnclesAndAunts(parent, parentX, parentY);
                }
            }
        }
    }

    private void addUnclesAndAunts(Person parent, int parentX, int parentY) {
        Set<Person> parentSiblings = new HashSet<>();
        Set<Person> grandParents = parent.getParents();
        
        for (Person grandParent : grandParents) {
            parentSiblings.addAll(grandParent.getChildren());
        }
        parentSiblings.remove(parent);

        if (!parentSiblings.isEmpty()) {
            List<Person> sortedUnclesAunts = new ArrayList<>(parentSiblings);
            sortedUnclesAunts.sort(Comparator.comparing(Person::getId));

            int leftCount = sortedUnclesAunts.size() / 2;
            int rightCount = sortedUnclesAunts.size() - leftCount;

            for (int i = 0; i < leftCount; i++) {
                if (!personPositions.containsKey(sortedUnclesAunts.get(i))) {
                    int uncleX = parentX - (leftCount - i) * (PERSON_WIDTH + HORIZONTAL_GAP);
                    personPositions.put(sortedUnclesAunts.get(i), new Point(uncleX, parentY));
                    updateBounds(uncleX, parentY);
                }
            }

            for (int i = 0; i < rightCount; i++) {
                Person uncleAunt = sortedUnclesAunts.get(leftCount + i);
                if (!personPositions.containsKey(uncleAunt)) {
                    int uncleX = parentX + (i + 1) * (PERSON_WIDTH + HORIZONTAL_GAP);
                    personPositions.put(uncleAunt, new Point(uncleX, parentY));
                    updateBounds(uncleX, parentY);
                }
            }

            if (!grandParents.isEmpty()) {
                int grandParentY = parentY - PERSON_HEIGHT - VERTICAL_GAP;
                List<Person> sortedGrandParents = new ArrayList<>(grandParents);
                sortedGrandParents.sort(Comparator.comparing(Person::getId));

                int totalWidth = sortedGrandParents.size() * PERSON_WIDTH + (sortedGrandParents.size() - 1) * HORIZONTAL_GAP;
                int startX = parentX - totalWidth / 2;

                for (int i = 0; i < sortedGrandParents.size(); i++) {
                    Person grandParent = sortedGrandParents.get(i);
                    if (!personPositions.containsKey(grandParent)) {
                        int x = startX + i * (PERSON_WIDTH + HORIZONTAL_GAP);
                        personPositions.put(grandParent, new Point(x, grandParentY));
                        updateBounds(x, grandParentY);
                    }
                }
            }
        }
    }

    private void calculateAncestorsView(int startX, int startY) {
        personPositions.put(rootPerson, new Point(startX, startY));
        calculateParentPositions(rootPerson, startX, startY);
    }

    private void calculateParentPositions(Person person, int personX, int personY) {
        if (person.getParents().isEmpty()) return;

        int parentY = personY - PERSON_HEIGHT - VERTICAL_GAP;
        List<Person> parents = new ArrayList<>(person.getParents());
        
        parents.sort(Comparator.comparing(Person::getId));
        
        int totalWidth = parents.size() * PERSON_WIDTH + (parents.size() - 1) * HORIZONTAL_GAP;
        int startX = personX + PERSON_WIDTH/2 - totalWidth/2;

        List<Integer> parentPositionsX = new ArrayList<>();
        for (int i = 0; i < parents.size(); i++) {
            Person parent = parents.get(i);
            int suggestedX = startX + i * (PERSON_WIDTH + HORIZONTAL_GAP);
            
            if (personPositions.containsKey(parent)) {
                Point existingPos = personPositions.get(parent);
                parentPositionsX.add(existingPos.x);
            } else {
                int availableX = findNextAvailablePosition(parentY, suggestedX, parentPositionsX);
                parentPositionsX.add(availableX);
            }
        }

        for (int i = 0; i < parents.size(); i++) {
            Person parent = parents.get(i);
            int parentX = parentPositionsX.get(i);
            
            if (!personPositions.containsKey(parent)) {
                personPositions.put(parent, new Point(parentX, parentY));
                updateBounds(parentX, parentY);
                calculateParentPositions(parent, parentX, parentY);
            }
        }
    }

    private int findNextAvailablePosition(int y, int suggestedX, List<Integer> existingPositions) {
        int x = suggestedX;
        boolean foundPosition = false;
        
        while (!foundPosition) {
            boolean overlaps = false;
            
            for (int existingX : existingPositions) {
                if (Math.abs(x - existingX) < PERSON_WIDTH + HORIZONTAL_GAP) {
                    overlaps = true;
                    break;
                }
            }
            
            for (Point point : personPositions.values()) {
                if (point.y == y && Math.abs(x - point.x) < PERSON_WIDTH + HORIZONTAL_GAP) {
                    overlaps = true;
                    break;
                }
            }
            
            if (!overlaps) {
                foundPosition = true;
            } else {
                x += HORIZONTAL_GAP;
            }
        }
        
        return x;
    }

    private void addToLevel(int y, int x) {
        Set<Integer> positions = levelPositions.computeIfAbsent(y, k -> new HashSet<>());
        for (int i = x; i < x + PERSON_WIDTH; i++) {
            positions.add(i);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (rootPerson == null) return;

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.setStroke(new BasicStroke(2));
        for (Map.Entry<Person, Point> entry : personPositions.entrySet()) {
            Person person = entry.getKey();
            Point childPoint = entry.getValue();
            
            if (showSiblingsMode) {
                for (Person parent : person.getParents()) {
                    Point parentPoint = personPositions.get(parent);
                    if (parentPoint != null) {
                        g2d.setColor(Color.GRAY);
                        g2d.drawLine(
                            childPoint.x + PERSON_WIDTH/2, childPoint.y,
                            parentPoint.x + PERSON_WIDTH/2, parentPoint.y + PERSON_HEIGHT
                        );
                    }
                }
            } else {
                for (Person parent : person.getParents()) {
                    Point parentPoint = personPositions.get(parent);
                    if (parentPoint != null) {
                        g2d.setColor(Color.GRAY);
                        g2d.drawLine(
                            childPoint.x + PERSON_WIDTH/2, childPoint.y,
                            parentPoint.x + PERSON_WIDTH/2, parentPoint.y + PERSON_HEIGHT
                        );
                    }
                }
            }
        }

        for (Map.Entry<Person, Point> entry : personPositions.entrySet()) {
            Person person = entry.getKey();
            Point point = entry.getValue();
            
            boolean isRoot = person == rootPerson;
            drawPerson(g2d, person, point.x, point.y, isRoot);
        }
    }

    private void drawPerson(Graphics2D g2d, Person person, int x, int y, boolean isRoot) {
        Color startColor = isRoot ? new Color(255, 240, 240) : new Color(240, 240, 255);
        Color endColor = isRoot ? new Color(240, 220, 220) : new Color(220, 220, 240);
        GradientPaint gradient = new GradientPaint(
            x, y, startColor,
            x, y + PERSON_HEIGHT, endColor
        );
        g2d.setPaint(gradient);
        g2d.fillRoundRect(x, y, PERSON_WIDTH, PERSON_HEIGHT, 10, 10);
        g2d.setColor(isRoot ? new Color(200, 180, 180) : new Color(180, 180, 200));
        g2d.drawRoundRect(x, y, PERSON_WIDTH, PERSON_HEIGHT, 10, 10);

        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        FontMetrics fm = g2d.getFontMetrics();
        String name = person.getFirstName() + " " + person.getLastName();
        String birthDate = person.getBirthDate() != null ? person.getBirthDate() : "";

        int nameWidth = fm.stringWidth(name);
        int dateWidth = fm.stringWidth(birthDate);
        
        g2d.drawString(name, x + (PERSON_WIDTH - nameWidth)/2, y + PERSON_HEIGHT/2);
        g2d.drawString(birthDate, x + (PERSON_WIDTH - dateWidth)/2, y + PERSON_HEIGHT/2 + fm.getHeight());
    }
}