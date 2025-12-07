// === ELEMENTI ===
const loginSection = document.getElementById("loginSection");
const registerSection = document.getElementById("registerSection");

const msg = document.getElementById("msg");
const msgRegister = document.getElementById("msgRegister");

// Campi registrazione
const newUsername     = document.getElementById("newUsername");
const newPassword     = document.getElementById("newPassword");
const confirmPassword = document.getElementById("confirmPassword");

// API corrette Sprint 3
const API_REGISTER = "http://localhost:8080/api/users/register";
const API_LOGIN    = "http://localhost:8080/api/users/login";


// === SWITCH INTERFACCE ===
document.getElementById("showRegister").addEventListener("click", (e) => {
  e.preventDefault();
  loginSection.classList.add("hidden");
  registerSection.classList.remove("hidden");
});

document.getElementById("showLogin").addEventListener("click", (e) => {
  e.preventDefault();
  registerSection.classList.add("hidden");
  loginSection.classList.remove("hidden");
});


// === REGISTRAZIONE ===
document.getElementById("registerBtn").addEventListener("click", async () => {
  const username = newUsername.value.trim().toLowerCase();
  const password = newPassword.value.trim();
  const confirm  = confirmPassword.value.trim();

  if (!username || !password || !confirm) {
    msgRegister.textContent = "⚠️ Tutti i campi sono obbligatori.";
    msgRegister.style.color = "#e58500";
    return;
  }

  if (password !== confirm) {
    msgRegister.textContent = "❌ Le password non coincidono.";
    msgRegister.style.color = "#d64d4d";
    return;
  }

  try {
    const res = await fetch(API_REGISTER, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username, password })
    });

    const data = await res.json();

    if (res.ok && data.success) {
      msgRegister.textContent = "✅ Registrazione completata!";
      msgRegister.style.color = "green";

      setTimeout(() => {
        registerSection.classList.add("hidden");
        loginSection.classList.remove("hidden");
      }, 1200);

    } else {
      msgRegister.textContent = `❌ ${data.message || "Errore durante la registrazione."}`;
      msgRegister.style.color = "#d64d4d";
    }

  } catch {
    msgRegister.textContent = "❌ Errore di connessione al server.";
    msgRegister.style.color = "#d64d4d";
  }
});


// === LOGIN ===
document.getElementById("loginBtn").addEventListener("click", async () => {
  const username = document.getElementById("username").value.trim().toLowerCase();
  const password = document.getElementById("password").value.trim();

  if (!username || !password) {
    msg.textContent = "⚠️ Inserisci username e password.";
    msg.style.color = "#e58500";
    return;
  }

  try {
    const res = await fetch(API_LOGIN, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username, password })
    });

    const data = await res.json();

    if (res.ok && data.success) {
      msg.textContent = "✅ Accesso effettuato!";
      msg.style.color = "green";

      // ⭐ Sprint 4: salva username validato dal backend
      localStorage.setItem("loggedUser", data.username);

      setTimeout(() => window.location.href = "dashboard.html", 900);

    } else {
      msg.textContent = `❌ ${data.message || "Credenziali errate."}`;
      msg.style.color = "#d64d4d";
    }

  } catch {
    msg.textContent = "❌ Errore di connessione al server.";
    msg.style.color = "#d64d4d";
  }
});
