
CREATE DATABASE IF NOT EXISTS hospital_management;

USE hospital_management;

-- 1. Patients
CREATE TABLE IF NOT EXISTS patients (
    patient_id INT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(150),
    disease VARCHAR(255),
    address TEXT,
    blood_group VARCHAR(10),
    emergency_contact VARCHAR(20)
);

-- 2. Doctors
CREATE TABLE IF NOT EXISTS doctors (
    doctor_id INT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(150),
    specialization VARCHAR(100),
    department VARCHAR(100),
    experience INT DEFAULT 0
);

-- 3. Appointments
CREATE TABLE IF NOT EXISTS appointments (
    appointment_id INT PRIMARY KEY,
    patient_id INT NOT NULL,
    doctor_id INT NOT NULL,
    appointment_date DATE NOT NULL,
    appointment_time TIME NOT NULL,
    status VARCHAR(30) DEFAULT 'Booked',

    FOREIGN KEY (patient_id)
        REFERENCES patients(patient_id),

    FOREIGN KEY (doctor_id)
        REFERENCES doctors(doctor_id)
);

-- 4. Medical records
CREATE TABLE IF NOT EXISTS medical_records (
    record_id INT PRIMARY KEY,
    patient_id INT NOT NULL UNIQUE,
    diagnosis TEXT,
    prescription TEXT,
    medical_history TEXT,
    record_date DATE,

    FOREIGN KEY (patient_id)
        REFERENCES patients(patient_id)
);

-- 5. Administrator accounts
CREATE TABLE IF NOT EXISTS administrators (
    admin_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL
);

SHOW TABLES;
