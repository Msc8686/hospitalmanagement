/* =====================================================
   ADMINISTRATOR DASHBOARD
   ===================================================== */

const API = "http://localhost:8080/api";


/* ---------- Date ---------- */

function todayString() {

    const now = new Date();

    return `${now.getFullYear()}-${
        String(now.getMonth() + 1).padStart(2, "0")
    }-${
        String(now.getDate()).padStart(2, "0")
    }`;
}


/* ---------- Helpers ---------- */

function escapeHtml(value) {

    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

async function fetchJson(url, options = {}) {

    const response = await fetch(url, options);
    const data = await response.json();

    if (!response.ok) {
        throw new Error(data.message || "Request failed.");
    }

    return data;
}


/* ---------- Login check ---------- */

function requireAdminLogin() {

    if (localStorage.getItem("adminLoggedIn") !== "true") {
        window.location.href = "index.html";
        return false;
    }

    return true;
}


/* ---------- Logout ---------- */

function logoutAdmin() {

    localStorage.removeItem("adminLoggedIn");

    window.location.href = "index.html";
}


/* ---------- Page startup ---------- */

document.addEventListener("DOMContentLoaded", () => {

    document.getElementById("addDoctorForm")
        .addEventListener("submit", addDoctor);

    document.getElementById("removeDoctorForm")
        .addEventListener("submit", removeDoctor);

    loadAdminDashboard();
});


/* ---------- Load all dashboard data ---------- */

async function loadAdminDashboard() {

    if (!requireAdminLogin()) return;

    try {

        const [
            report,
            patients,
            doctors,
            appointments
        ] = await Promise.all([

            fetchJson(`${API}/admin/report`),

            fetchJson(`${API}/admin/patients`),

            fetchJson(`${API}/admin/doctors`),

            fetchJson(`${API}/admin/appointments`)
        ]);

        document.getElementById("patientCount").textContent =
            report.patients;

        document.getElementById("doctorCount").textContent =
            report.doctors;

        document.getElementById("appointmentCount").textContent =
            report.appointments;

        document.getElementById("recordCount").textContent =
            report.records;

        renderPatients(patients);
        renderDoctors(doctors);

        renderAppointments(
            appointments,
            "allAppointments"
        );

        const todayAppointments = appointments.filter(
            item => item.date === todayString()
        );

        document.getElementById("todayCount").textContent =
            todayAppointments.length;

        renderAppointments(
            todayAppointments,
            "todayAppointments"
        );

    } catch (error) {

        alert(
            `Could not load the administrator dashboard. ` +
            `${error.message}\n` +
            `Check that HospitalServer is running.`
        );

        console.error(error);
    }
}


/* ---------- Show all patients ---------- */

function renderPatients(patients) {

    const container =
        document.getElementById("allPatients");

    if (!patients.length) {

        container.innerHTML =
            '<p class="empty-state">No patients have registered yet.</p>';

        return;
    }

    let html = `
        <div class="table-wrap">
            <table>
                <thead>
                    <tr>
                        <th>Patient ID</th>
                        <th>Name</th>
                        <th>Phone</th>
                        <th>Email</th>
                        <th>Health Problem</th>
                        <th>Blood Group</th>
                    </tr>
                </thead>
                <tbody>
    `;

    patients.forEach(patient => {

        html += `
            <tr>
                <td>${patient.id}</td>
                <td>${escapeHtml(patient.name)}</td>
                <td>${escapeHtml(patient.phone)}</td>
                <td>${escapeHtml(patient.email)}</td>
                <td>${escapeHtml(patient.disease)}</td>
                <td>${escapeHtml(patient.bloodGroup)}</td>
            </tr>
        `;
    });

    container.innerHTML =
        html + "</tbody></table></div>";
}


/* ---------- Show all doctors ---------- */

function renderDoctors(doctors) {

    const container =
        document.getElementById("allDoctors");

    if (!doctors.length) {

        container.innerHTML =
            '<p class="empty-state">No doctors found.</p>';

        return;
    }

    let html = `
        <div class="table-wrap">
            <table>
                <thead>
                    <tr>
                        <th>Doctor ID</th>
                        <th>Name</th>
                        <th>Specialization</th>
                        <th>Department</th>
                        <th>Experience</th>
                    </tr>
                </thead>
                <tbody>
    `;

    doctors.forEach(doctor => {

        html += `
            <tr>
                <td>${doctor.id}</td>
                <td>${escapeHtml(doctor.name)}</td>
                <td>${escapeHtml(doctor.specialization)}</td>
                <td>${escapeHtml(doctor.department)}</td>
                <td>${doctor.experience} years</td>
            </tr>
        `;
    });

    container.innerHTML =
        html + "</tbody></table></div>";
}


/* ---------- Show appointments ---------- */

function renderAppointments(appointments, containerId) {

    const container =
        document.getElementById(containerId);

    if (!appointments.length) {

        container.innerHTML =
            '<p class="empty-state">No appointments found for this view.</p>';

        return;
    }

    let html = `
        <div class="table-wrap">
            <table>
                <thead>
                    <tr>
                        <th>Appointment ID</th>
                        <th>Patient</th>
                        <th>Patient ID</th>
                        <th>Doctor</th>
                        <th>Date</th>
                        <th>Time</th>
                        <th>Status</th>
                    </tr>
                </thead>
                <tbody>
    `;

    appointments.forEach(appointment => {

        html += `
            <tr>
                <td>${appointment.id}</td>
                <td>${escapeHtml(appointment.patient)}</td>
                <td>${appointment.patientId}</td>
                <td>${escapeHtml(appointment.doctor)}</td>
                <td>${escapeHtml(appointment.date)}</td>
                <td>${escapeHtml(appointment.time)}</td>
                <td>${escapeHtml(appointment.status)}</td>
            </tr>
        `;
    });

    container.innerHTML =
        html + "</tbody></table></div>";
}


/* ---------- Add a doctor ---------- */

async function addDoctor(event) {

    event.preventDefault();

    const form =
        document.getElementById("addDoctorForm");

    const feedback =
        document.getElementById("addDoctorFeedback");

    try {

        const result = await fetchJson(
            `${API}/admin/add-doctor`,
            {
                method: "POST",

                headers: {
                    "Content-Type":
                        "application/x-www-form-urlencoded"
                },

                body: new URLSearchParams(
                    new FormData(form)
                )
            }
        );

        feedback.className = "feedback success";

        feedback.textContent =
            `${result.message} ID: ${result.doctorId}. ` +
            `Login: ${result.username}, ` +
            `password: ${result.password}`;

        form.reset();

        await loadAdminDashboard();

    } catch (error) {

        feedback.className = "feedback error";
        feedback.textContent = error.message;
    }
}


/* ---------- Remove a doctor ---------- */

async function removeDoctor(event) {

    event.preventDefault();

    const form =
        document.getElementById("removeDoctorForm");

    const feedback =
        document.getElementById("removeDoctorFeedback");

    try {

        const result = await fetchJson(
            `${API}/admin/remove-doctor`,
            {
                method: "POST",

                headers: {
                    "Content-Type":
                        "application/x-www-form-urlencoded"
                },

                body: new URLSearchParams(
                    new FormData(form)
                )
            }
        );

        feedback.className = "feedback success";
        feedback.textContent = result.message;

        form.reset();

        await loadAdminDashboard();

    } catch (error) {

        feedback.className = "feedback error";
        feedback.textContent = error.message;
    }
}