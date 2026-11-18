package com.timetabler.util;

import com.timetabler.model.*;

/**
 * Creates realistic, production-like college and university datasets.
 */
public class SampleDataGenerator {

    public static TimetableData createRealisticUniversityDataset() {
        TimetableData data = new TimetableData();

        // 1. Time Slots (Monday - Friday, 6 active periods per day, lunch break 12-13)
        DayOfWeek[] days = DayOfWeek.values();
        String[][] periodTimes = {
                {"09:00", "10:00"},
                {"10:00", "11:00"},
                {"11:00", "12:00"},
                {"13:00", "14:00"},
                {"14:00", "15:00"},
                {"15:00", "16:00"}
        };

        for (DayOfWeek day : days) {
            for (int p = 0; p < periodTimes.length; p++) {
                String slotId = day.getShortName().toUpperCase() + "_P" + (p + 1);
                data.addTimeSlot(new TimeSlot(slotId, day, p, periodTimes[p][0], periodTimes[p][1]));
            }
        }

        // 2. Rooms
        data.addRoom(new Room("R_AUD1", "Main Auditorium (Aud-1)", 120, RoomType.LECTURE_HALL));
        data.addRoom(new Room("R_LH1", "Lecture Hall Alpha (LH-1)", 75, RoomType.LECTURE_HALL));
        data.addRoom(new Room("R_CR1", "Classroom 201 (CR-1)", 50, RoomType.CLASSROOM));
        data.addRoom(new Room("R_CR2", "Classroom 202 (CR-2)", 50, RoomType.CLASSROOM));
        data.addRoom(new Room("R_CR3", "Classroom 203 (CR-3)", 50, RoomType.CLASSROOM));
        data.addRoom(new Room("R_LAB1", "Software Computing Lab (Lab-1)", 48, RoomType.COMPUTER_LAB));
        data.addRoom(new Room("R_LAB2", "Data Science & AI Lab (Lab-2)", 48, RoomType.COMPUTER_LAB));
        data.addRoom(new Room("R_LAB3", "Electronics & Embedded Lab (Lab-3)", 45, RoomType.SCIENCE_LAB));

        // 3. Courses
        Course cs101 = new Course("CS101", "CS101", "Programming Fundamentals", 3, false, RoomType.CLASSROOM);
        Course cs102 = new Course("CS102", "CS102", "Data Structures & Algorithms", 3, false, RoomType.CLASSROOM);
        Course cs103 = new Course("CS103", "CS103", "Object-Oriented Java Programming", 3, false, RoomType.CLASSROOM);
        Course cs104 = new Course("CS104", "CS104", "Java & OOP Lab", 2, true, RoomType.COMPUTER_LAB);

        Course cs201 = new Course("CS201", "CS201", "Operating Systems", 3, false, RoomType.CLASSROOM);
        Course cs202 = new Course("CS202", "CS202", "Database Management Systems", 3, false, RoomType.CLASSROOM);
        Course cs203 = new Course("CS203", "CS203", "DBMS & SQL Lab", 2, true, RoomType.COMPUTER_LAB);

        Course cs301 = new Course("CS301", "CS301", "Computer Networks & Security", 3, false, RoomType.CLASSROOM);
        Course cs302 = new Course("CS302", "CS302", "Artificial Intelligence & ML", 3, false, RoomType.CLASSROOM);
        Course cs303 = new Course("CS303", "CS303", "Machine Learning Lab", 2, true, RoomType.COMPUTER_LAB);

        Course it101 = new Course("IT101", "IT101", "Full-Stack Web Development", 3, false, RoomType.CLASSROOM);
        Course it102 = new Course("IT102", "IT102", "Web Technologies Lab", 2, true, RoomType.COMPUTER_LAB);

        Course ec101 = new Course("EC101", "EC101", "Digital Logic & Microprocessors", 3, false, RoomType.CLASSROOM);
        Course ec102 = new Course("EC102", "EC102", "Microprocessor Hardware Lab", 2, true, RoomType.SCIENCE_LAB);

        Course ma101 = new Course("MA101", "MA101", "Discrete Math & Linear Algebra", 3, false, RoomType.CLASSROOM);

        data.addCourse(cs101);
        data.addCourse(cs102);
        data.addCourse(cs103);
        data.addCourse(cs104);
        data.addCourse(cs201);
        data.addCourse(cs202);
        data.addCourse(cs203);
        data.addCourse(cs301);
        data.addCourse(cs302);
        data.addCourse(cs303);
        data.addCourse(it101);
        data.addCourse(it102);
        data.addCourse(ec101);
        data.addCourse(ec102);
        data.addCourse(ma101);

        // 4. Instructors
        Instructor inst1 = new Instructor("INS_TURING", "Dr. Alan Turing", "turing@university.edu", "Computer Science", 4);
        inst1.addQualifiedCourse("CS101");
        inst1.addQualifiedCourse("CS102");
        inst1.addQualifiedCourse("CS302");

        Instructor inst2 = new Instructor("INS_LOVELACE", "Prof. Ada Lovelace", "lovelace@university.edu", "Computer Science", 4);
        inst2.addQualifiedCourse("CS103");
        inst2.addQualifiedCourse("CS104");
        inst2.addQualifiedCourse("IT101");

        Instructor inst3 = new Instructor("INS_KNUTH", "Dr. Donald Knuth", "knuth@university.edu", "Computer Science", 3);
        inst3.addQualifiedCourse("CS102");
        inst3.addQualifiedCourse("MA101");
        inst3.addQualifiedCourse("CS201");

        Instructor inst4 = new Instructor("INS_HOPPER", "Prof. Grace Hopper", "hopper@university.edu", "Computer Science", 4);
        inst4.addQualifiedCourse("CS103");
        inst4.addQualifiedCourse("CS201");
        inst4.addQualifiedCourse("CS202");

        Instructor inst5 = new Instructor("INS_SHANNON", "Dr. Claude Shannon", "shannon@university.edu", "Electronics", 4);
        inst5.addQualifiedCourse("CS301");
        inst5.addQualifiedCourse("EC101");
        inst5.addQualifiedCourse("EC102");

        Instructor inst6 = new Instructor("INS_LISKOV", "Prof. Barbara Liskov", "liskov@university.edu", "Computer Science", 4);
        inst6.addQualifiedCourse("CS103");
        inst6.addQualifiedCourse("CS104");
        inst6.addQualifiedCourse("CS202");
        inst6.addQualifiedCourse("CS203");

        Instructor inst7 = new Instructor("INS_BERNERS", "Dr. Tim Berners-Lee", "berners@university.edu", "Information Tech", 4);
        inst7.addQualifiedCourse("IT101");
        inst7.addQualifiedCourse("IT102");
        inst7.addQualifiedCourse("CS301");

        Instructor inst8 = new Instructor("INS_HINTON", "Prof. Geoffrey Hinton", "hinton@university.edu", "AI & Data Science", 3);
        inst8.addQualifiedCourse("CS302");
        inst8.addQualifiedCourse("CS303");
        inst8.addQualifiedCourse("MA101");

        Instructor inst9 = new Instructor("INS_JOHNSON", "Dr. Katherine Johnson", "johnson@university.edu", "Mathematics", 4);
        inst9.addQualifiedCourse("MA101");
        inst9.addQualifiedCourse("CS101");

        data.addInstructor(inst1);
        data.addInstructor(inst2);
        data.addInstructor(inst3);
        data.addInstructor(inst4);
        data.addInstructor(inst5);
        data.addInstructor(inst6);
        data.addInstructor(inst7);
        data.addInstructor(inst8);
        data.addInstructor(inst9);

        // 5. Student Groups (Cohorts / Batches)
        StudentGroup cse1 = new StudentGroup("GRP_CSE_Y1", "CSE Year 1 (Sec-A)", 42);
        cse1.enrollCourse("CS101");
        cse1.enrollCourse("CS103");
        cse1.enrollCourse("CS104");
        cse1.enrollCourse("MA101");

        StudentGroup cse2 = new StudentGroup("GRP_CSE_Y2", "CSE Year 2 (Sec-A)", 38);
        cse2.enrollCourse("CS102");
        cse2.enrollCourse("CS201");
        cse2.enrollCourse("CS202");
        cse2.enrollCourse("CS203");

        StudentGroup cse3 = new StudentGroup("GRP_CSE_Y3", "CSE Year 3 (Sec-A)", 35);
        cse3.enrollCourse("CS301");
        cse3.enrollCourse("CS302");
        cse3.enrollCourse("CS303");
        cse3.enrollCourse("IT101");

        StudentGroup it1 = new StudentGroup("GRP_IT_Y1", "IT Year 1 (Sec-A)", 40);
        it1.enrollCourse("CS101");
        it1.enrollCourse("IT101");
        it1.enrollCourse("IT102");
        it1.enrollCourse("MA101");

        StudentGroup ece1 = new StudentGroup("GRP_ECE_Y1", "ECE Year 1 (Sec-A)", 36);
        ece1.enrollCourse("CS101");
        ece1.enrollCourse("EC101");
        ece1.enrollCourse("EC102");
        ece1.enrollCourse("MA101");

        data.addStudentGroup(cse1);
        data.addStudentGroup(cse2);
        data.addStudentGroup(cse3);
        data.addStudentGroup(it1);
        data.addStudentGroup(ece1);

        // 6. Generate individual class sessions based on course weekly lecture count
        data.generateClassSessions();

        return data;
    }
}
