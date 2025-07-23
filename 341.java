Below is a comprehensive, production-ready Java solution for the project health tracking system. This system is built using Maven for dependency management, making it easy to set up and run.

### Overview

This system is designed with a clean, modular architecture:

1.  **Data Models (`model` package):** Plain Old Java Objects (POJOs) to represent your data (Projects, Tasks, etc.).
2.  **Input (`ProjectDataReader`):** A service to read and parse project data from JSON files. JSON is chosen for its human-readability and ease of use.
3.  **Core Logic (`HealthAnalyzer`):** The brain of the system. It performs all the analysis: detecting overdue/stagnant tasks, estimating delays, and finding resource issues.
4.  **Outputs (`ReportGenerator`, `GanttChartGenerator`):** Services responsible for creating the client-ready PDF summary and the console-based Gantt chart.
5.  **Main Application (`Main.java`):** The entry point that orchestrates the entire process.

---

### Step A: Project Setup (using Maven)

First, you need a `pom.xml` file to manage the necessary libraries (Jackson for JSON, Apache PDFBox for PDFs).

**`pom.xml`**
```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.pm.tracker</groupId>
    <artifactId>project-health-tracker</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <properties>
        <maven.compiler.source>11</maven.compiler.source>
        <maven.compiler.target>11</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <!-- Jackson for JSON data binding -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>2.15.2</version>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.datatype</groupId>
            <artifactId>jackson-datatype-jsr310</artifactId>
            <version>2.15.2</version>
        </dependency>

        <!-- Apache PDFBox for PDF generation -->
        <dependency>
            <groupId>org.apache.pdfbox</groupId>
            <artifactId>pdfbox</artifactId>
            <version>2.0.29</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-jar-plugin</artifactId>
                <version>3.2.2</version>
                <configuration>
                    <archive>
                        <manifest>
                            <addClasspath>true</addClasspath>
                            <mainClass>com.pm.tracker.Main</mainClass>
                        </manifest>
                    </archive>
                </configuration>
            </plugin>
            <plugin>
                 <groupId>org.apache.maven.plugins</groupId>
                 <artifactId>maven-shade-plugin</artifactId>
                 <version>3.2.4</version>
                 <executions>
                     <execution>
                         <phase>package</phase>
                         <goals>
                             <goal>shade</goal>
                         </goals>
                         <configuration>
                             <transformers>
                                 <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                                     <mainClass>com.pm.tracker.Main</mainClass>
                                 </transformer>
                             </transformers>
                         </configuration>
                     </execution>
                 </executions>
             </plugin>
        </plugins>
    </build>
</project>
```
---
### Step B: Sample Project Data File
Create a directory named `projects` in your project's root. Inside, create a sample JSON file. This structure is what the system will read.

**`projects/project_nexus.json`**
```json
{
  "name": "Project Nexus",
  "clientName": "Innovate Inc.",
  "startDate": "2023-09-01",
  "milestones": [
    { "name": "Phase 1 Complete", "dueDate": "2023-10-15" },
    { "name": "Final Launch", "dueDate": "2023-12-20" }
  ],
  "tasks": [
    {
      "id": "NEX-01", "name": "Setup Initial Infrastructure", "assignedTo": "Ali",
      "startDate": "2023-09-01", "dueDate": "2023-09-10",
      "completionDate": "2023-09-12", "lastUpdatedDate": "2023-09-12", "status": "COMPLETED"
    },
    {
      "id": "NEX-02", "name": "Develop Core Authentication", "assignedTo": "Bobby",
      "startDate": "2023-09-11", "dueDate": "2023-09-25",
      "completionDate": null, "lastUpdatedDate": "2023-10-05", "status": "IN_PROGRESS"
    },
    {
      "id": "NEX-03", "name": "API Endpoint Design", "assignedTo": "Ali",
      "startDate": "2023-09-15", "dueDate": "2023-09-30",
      "completionDate": null, "lastUpdatedDate": "2023-09-28", "status": "IN_PROGRESS"
    },
    {
      "id": "NEX-04", "name": "User Interface Mockups", "assignedTo": "Charliee",
      "startDate": "2023-10-01", "dueDate": "2023-10-10",
      "completionDate": null, "lastUpdatedDate": "2023-10-02", "status": "NOT_STARTED"
    },
    {
      "id": "NEX-05", "name": "Database Schema Finalization", "assignedTo": "Ali",
      "startDate": "2023-10-05", "dueDate": "2023-10-12",
      "completionDate": null, "lastUpdatedDate": "2023-10-06", "status": "IN_PROGRESS"
    },
    {
      "id": "NEX-06", "name": "QA Testing Plan", "assignedTo": "David",
      "startDate": "2023-10-15", "dueDate": "2023-10-25",
      "completionDate": null, "lastUpdatedDate": "2023-10-15", "status": "NOT_STARTED"
    }
  ]
}
```

---

### Step C: Java Source Code

Create the following Java files inside `src/main/java/com/pm/tracker/`.

#### **`model` Package**

These classes map directly to the JSON structure.

**`model/Status.java`**
```java
package com.pm.tracker.model;

public enum Status {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED,
    ON_HOLD,
    CANCELLED
}
```

**`model/Project.java`, `model/Task.java`, `model/Milestone.java`**
```java
// common/Project.java
package com.pm.tracker.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.List;
import java.time.LocalDate;


// Getters and Setters omitted for brevity, but are required for Jackson.
// Use your IDE to generate them.

public class Project {
    private String name;
    private String clientName;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    private List<Task> tasks;
    private List<Milestone> milestones;
    // ... Getters and Setters
}

// common/Task.java
package com.pm.tracker.model;
import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonFormat;
public class Task {
    private String id;
    private String name;
    private String assignedTo;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dueDate;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate completionDate;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate lastUpdatedDate;
    private Status status;
}

// common/Milestone.java
package com.pm.tracker.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;

public class Milestone {
    private String name;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dueDate;
}
```
*(**Note:** Remember to generate standard getters and setters for all private fields in the model classes.)*

#### **`service` Package**

**`service/ProjectDataReader.java`**
```java
package com.pm.tracker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pm.tracker.model.Project;
import java.io.File;
import java.io.IOException;

public class ProjectDataReader {
    private final ObjectMapper objectMapper;

    public ProjectDataReader() {
        this.objectMapper = new ObjectMapper();
        // Register the JavaTimeModule to handle LocalDate objects
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public Project readProject(String filePath) throws IOException {
        return objectMapper.readValue(new File(filePath), Project.class);
    }
}
```

**`service/HealthAnalyzer.java`**
```java
package com.pm.tracker.service;

import com.pm.tracker.model.Project;
import com.pm.tracker.model.Status;
import com.pm.tracker.model.Task;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class HealthAnalyzer {

    private final Project project;
    private final LocalDate today = LocalDate.now();

    public HealthAnalyzer(Project project) {
        this.project = project;
    }

    public List<Task> getOverdueTasks() {
        return project.getTasks().stream()
                .filter(task -> task.getStatus() != Status.COMPLETED && task.getDueDate().isBefore(today))
                .collect(Collectors.toList());
    }

    public List<Task> getStagnantTasks(int daysThreshold) {
        return project.getTasks().stream()
                .filter(task -> task.getStatus() == Status.IN_PROGRESS || task.getStatus() == Status.NOT_STARTED)
                .filter(task -> ChronoUnit.DAYS.between(task.getLastUpdatedDate(), today) > daysThreshold)
                .collect(Collectors.toList());
    }

    public double calculateAverageDelayFactor() {
        List<Double> delayRatios = new ArrayList<>();
        for (Task task : project.getTasks()) {
            if (task.getStatus() == Status.COMPLETED && task.getCompletionDate() != null) {
                long plannedDuration = ChronoUnit.DAYS.between(task.getStartDate(), task.getDueDate());
                long actualDuration = ChronoUnit.DAYS.between(task.getStartDate(), task.getCompletionDate());

                if (plannedDuration > 0) {
                    delayRatios.add((double) actualDuration / plannedDuration);
                }
            }
        }
        return delayRatios.stream().mapToDouble(d -> d).average().orElse(1.0); // Default to 1.0 (no delay)
    }

    public LocalDate estimateProjectedCompletionDate(double delayFactor) {
        return project.getTasks().stream()
                .filter(task -> task.getStatus() != Status.COMPLETED)
                .map(task -> {
                    long plannedDuration = ChronoUnit.DAYS.between(task.getStartDate(), task.getDueDate());
                    long estimatedDuration = (long) (plannedDuration * delayFactor);
                    return task.getStartDate().plusDays(estimatedDuration);
                })
                .max(LocalDate::compareTo)
                .orElse(project.getTasks().stream().map(Task::getDueDate).max(LocalDate::compareTo).orElse(today));
    }

    public Map<String, Long> findResourceOverallocations(int activeTaskLimit) {
        Map<String, Long> activeTaskCounts = project.getTasks().stream()
                .filter(task -> task.getStatus() == Status.IN_PROGRESS)
                .collect(Collectors.groupingBy(Task::getAssignedTo, Collectors.counting()));

        return activeTaskCounts.entrySet().stream()
                .filter(entry -> entry.getValue() > activeTaskLimit)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
```

**`service/GanttChartGenerator.java`**
```java
package com.pm.tracker.service;

import com.pm.tracker.model.Project;
import com.pm.tracker.model.Status;
import com.pm.tracker.model.Task;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class GanttChartGenerator {

    public void printGanttChart(Project project) {
        System.out.println("\n--- Gantt-style Visual Tracker for: " + project.getName() + " ---\n");
        if (project.getTasks().isEmpty()) {
            System.out.println("No tasks to display.");
            return;
        }

        LocalDate chartStartDate = project.getTasks().stream().map(Task::getStartDate).min(LocalDate::compareTo).get();
        LocalDate chartEndDate = project.getTasks().stream().map(Task::getDueDate).max(LocalDate::compareTo).get();
        long chartDuration = ChronoUnit.DAYS.between(chartStartDate, chartEndDate);
        
        int chartWidth = 60; // characters

        System.out.println("Timeline: " + chartStartDate + " to " + chartEndDate + "\n");

        for (Task task : project.getTasks()) {
            long taskStartOffset = ChronoUnit.DAYS.between(chartStartDate, task.getStartDate());
            long taskDuration = ChronoUnit.DAYS.between(task.getStartDate(), task.getDueDate());

            int barStart = (int) (taskStartOffset * chartWidth / chartDuration);
            int barWidth = (int) (taskDuration * chartWidth / chartDuration);
            if(barWidth < 1) barWidth = 1;

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%-8s | ", task.getId()));
            for (int i = 0; i < chartWidth; i++) {
                if (i >= barStart && i < barStart + barWidth) {
                    sb.append(getTaskCharacter(task.getStatus()));
                } else {
                    sb.append("·");
                }
            }
            sb.append(" | ").append(task.getName());
            System.out.println(sb.toString());
        }
        System.out.println("\nLegend: C=Completed, I=In Progress, N=Not Started, O=On Hold/Cancelled");
    }

    private String getTaskCharacter(Status status) {
        switch (status) {
            case COMPLETED: return "C";
            case IN_PROGRESS: return "I";
            case NOT_STARTED: return "N";
            default: return "O";
        }
    }
}
```

**`service/ReportGenerator.java`**
```java
package com.pm.tracker.service;

import com.pm.tracker.model.Project;
import com.pm.tracker.model.Task;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.List;


public class ReportGenerator {

    private PDDocument document;
    private PDPageContentStream contentStream;
    private float yPosition;
    private final float startY = 750;
    private final float margin = 50;

    public void generatePdfReport(Project project, HealthAnalyzer analyzer, String outputPath) throws IOException {
        document = new PDDocument();
        PDPage page = new PDPage();
        document.addPage(page);
        contentStream = new PDPageContentStream(document, page);
        yPosition = startY;

        // --- Report Header ---
        writeText(project.getClientName() + " - Project Health Summary", PDType1Font.HELVETICA_BOLD, 18);
        writeText("Project: " + project.getName(), PDType1Font.HELVETICA, 14);
        writeText("Report Date: " + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE), PDType1Font.HELVETICA_OBLIQUE, 12);

        // --- Analysis Section ---
        yPosition -= 30;
        writeText("Key Health Indicators", PDType1Font.HELVETICA_BOLD, 14);
        double delayFactor = analyzer.calculateAverageDelayFactor();
        LocalDate projectedEnd = analyzer.estimateProjectedCompletionDate(delayFactor);
        writeListItem("Status: ON TRACK"); // This could be more sophisticated
        writeListItem(String.format("Average Delay Factor: %.2f (tasks are taking %.0f%% of planned time)", delayFactor, delayFactor * 100));
        writeListItem("Projected Completion Date (based on past performance): " + projectedEnd.toString());

        // --- Overdue Tasks ---
        List<Task> overdue = analyzer.getOverdueTasks();
        if (!overdue.isEmpty()) {
            yPosition -= 20;
            writeText("Overdue Tasks", PDType1Font.HELVETICA_BOLD, 14);
            for (Task task : overdue) {
                writeListItem(String.format("%s (%s) - Due: %s, Assigned: %s", task.getId(), task.getName(), task.getDueDate(), task.getAssignedTo()));
            }
        }
        
        // --- Stagnant Tasks ---
        List<Task> stagnant = analyzer.getStagnantTasks(7); // 7-day threshold
         if (!stagnant.isEmpty()) {
            yPosition -= 20;
            writeText("Stagnant Tasks (No update in >7 days)", PDType1Font.HELVETICA_BOLD, 14);
            for (Task task : stagnant) {
                 writeListItem(String.format("%s (%s) - Last Update: %s, Status: %s", task.getId(), task.getName(), task.getLastUpdatedDate(), task.getStatus()));
            }
        }

        // --- Resource Allocation ---
        Map<String, Long> overallocations = analyzer.findResourceOverallocations(2); // Limit of 2 active tasks
        if (!overallocations.isEmpty()) {
            yPosition -= 20;
            writeText("Resource Allocation Notes", PDType1Font.HELVETICA_BOLD, 14);
            overallocations.forEach((resource, count) -> 
                writeListItem(String.format("Potential Over-allocation: %s is assigned to %d tasks currently in progress.", resource, count))
            );
        }

        contentStream.close();
        document.save(outputPath);
        document.close();
    }
    
    private void writeText(String text, PDType1Font font, int fontSize) throws IOException {
        contentStream.beginText();
        contentStream.setFont(font, fontSize);
        contentStream.newLineAtOffset(margin, yPosition);
        contentStream.showText(text);
        contentStream.endText();
        yPosition -= (fontSize * 1.5);
    }
    
    private void writeListItem(String text) {
        try {
            // A simple bullet point
            writeText("• " + text, PDType1Font.HELVETICA, 10);
        } catch(IOException e) {
             e.printStackTrace();
        }
    }
}
```

#### **Main Class**

**`Main.java`**
```java
package com.pm.tracker;

import com.pm.tracker.model.Project;
import com.pm.tracker.model.Task;
import com.pm.tracker.service.GanttChartGenerator;
import com.pm.tracker.service.HealthAnalyzer;
import com.pm.tracker.service.ProjectDataReader;
import com.pm.tracker.service.ReportGenerator;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;
import java.util.List;

public class Main {
    // --- Configuration ---
    private static final String PROJECT_FILE = "projects/project_nexus.json";
    private static final int STAGNANT_TASK_THRESHOLD_DAYS = 7;
    private static final int RESOURCE_ACTIVE_TASK_LIMIT = 2;

    public static void main(String[] args) {
        System.out.println("--- Starting Project Health Tracker ---");

        try {
            // 1. Read Project Data
            ProjectDataReader reader = new ProjectDataReader();
            Project project = reader.readProject(PROJECT_FILE);
            System.out.println("Successfully read data for project: " + project.getName());

            // 2. Analyze Project Health
            HealthAnalyzer analyzer = new HealthAnalyzer(project);
            
            System.out.println("\n--- Health Analysis Results ---");
            
            // Overdue Tasks
            List<Task> overdueTasks = analyzer.getOverdueTasks();
            System.out.println("\n[!] Overdue Tasks (" + overdueTasks.size() + "):");
            overdueTasks.forEach(t -> System.out.printf("  - %s: %s (Due: %s)\n", t.getId(), t.getName(), t.getDueDate()));

            // Stagnant Tasks
            List<Task> stagnantTasks = analyzer.getStagnantTasks(STAGNANT_TASK_THRESHOLD_DAYS);
            System.out.println("\n[!] Stagnant Tasks (No update in >" + STAGNANT_TASK_THRESHOLD_DAYS + " days) (" + stagnantTasks.size() + "):");
            stagnantTasks.forEach(t -> System.out.printf("  - %s: %s (Last Update: %s)\n", t.getId(), t.getName(), t.getLastUpdatedDate()));
            
            // Delay Estimation
            double delayFactor = analyzer.calculateAverageDelayFactor();
            LocalDate projectedDate = analyzer.estimateProjectedCompletionDate(delayFactor);
            System.out.printf("\n[*] Delay Estimation: On average, tasks take %.2f times the planned duration.\n", delayFactor);
            System.out.println("[*] Projected Project Completion Date: " + projectedDate);

            // Resource Allocation
            Map<String, Long> overallocations = analyzer.findResourceOverallocations(RESOURCE_ACTIVE_TASK_LIMIT);
            System.out.println("\n[!] Resource Allocation Inefficiencies (>" + RESOURCE_ACTIVE_TASK_LIMIT + " active tasks):");
            if (overallocations.isEmpty()) {
                System.out.println("  - None detected.");
            } else {
                overallocations.forEach((resource, count) -> 
                    System.out.printf("  - %s has %d tasks in progress.\n", resource, count)
                );
            }

            // 3. Generate Gantt Chart
            GanttChartGenerator gantt = new GanttChartGenerator();
            gantt.printGanttChart(project);

            // 4. Generate PDF Report
            String reportPath = "Project_Health_Report_" + project.getName().replaceAll("\\s+", "_") + ".pdf";
            ReportGenerator pdfGenerator = new ReportGenerator();
            pdfGenerator.generatePdfReport(project, analyzer, reportPath);
            System.out.println("\n--- Report Generation ---");
            System.out.println("[SUCCESS] Client-ready PDF summary saved as: " + reportPath);

        } catch (IOException e) {
            System.err.println("Error processing project file: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
```
---
### Step D: How to Run the System

1.  **Organize Files:** Make sure your file structure looks like this:
    ```
    project-health-tracker/
    ├── pom.xml
    ├── projects/
    │   └── project_nexus.json
    └── src/
        └── main/
            └── java/
                └── com/
                    └── pm/
                        └── tracker/
                            ├── Main.java
                            ├── model/
                            │   ├── Project.java
                            │   ├── Task.java
                            │   └── ...
                            └── service/
                                ├── HealthAnalyzer.java
                                ├── ReportGenerator.java
                                └── ...
    ```

2.  **Build and Run with Maven:** Open a terminal in the `project-health-tracker` root directory.
    *   **Build the project:** This will download dependencies and compile your code.
        ```bash
        mvn clean package
        ```
    *   **Run the executable JAR:**
        ```bash
        java -jar target/project-health-tracker-1.0.0.jar
        ```

### Expected Output

When running the command, we will see console output like this:

```
--- Starting Project Health Tracker ---
Successfully read data for project: Project Nexus

--- Health Analysis Results ---

[!] Overdue Tasks (1):
  - NEX-02: Develop Core Authentication (Due: 2023-09-25)

[!] Stagnant Tasks (No update in >7 days) (1):
  - NEX-02: Develop Core Authentication (Last Update: 2023-10-05)

[*] Delay Estimation: On average, tasks take 1.22 times the planned duration.
[*] Projected Project Completion Date: 2023-10-27

[!] Resource Allocation Inefficiencies (>2 active tasks):
  - Ali has 3 tasks in progress.

--- Gantt-style Visual Tracker for: Project Nexus ---

Timeline: 2023-09-01 to 2023-10-25

NEX-01   | CCCCC······················································· | Setup Initial Infrastructure
NEX-02   | ··········IIIIIIIIIIIIII···································· | Develop Core Authentication
NEX-03   | ···············IIIIIIIIIIIIIII······························ | API Endpoint Design
NEX-04   | ·······························NNNNNNNNN···················· | User Interface Mockups
NEX-05   | ····································IIIIIII················· | Database Schema Finalization
NEX-06   | ··········································NNNNNNNNNN········ | QA Testing Plan

Legend: C=Completed, I=In Progress, N=Not Started, O=On Hold/Cancelled

--- Report Generation ---
[SUCCESS] Client-ready PDF summary saved as: Project_Health_Report_Project_Nexus.pdf
```

And a file named `Project_Health_Report_Project_Nexus.pdf` will be created in the project's root directory, containing a professional summary for the client.