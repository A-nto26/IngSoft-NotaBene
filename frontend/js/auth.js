//  CONFIG SEZIONI
const loginSection = document.getElementById("loginSection");
const registerSection = document.getElementById("registerSection");
const showRegister = document.getElementById("showRegister");
const showLogin = document.getElementById("showLogin");
const msg = document.getElementById("msg");
const msgRegister = document.getElementById("msgRegister");

// API aggiornate
const API_REGISTER = "http://localhost:8080/api/users/register";
const API_LOGIN    = "http://localhost:8080/api/users/login";


//  CAMBIO SEZIONE
showRegister.addEventListener("click", (e) => {
  e.preventDefault();
  loginSection.classList.add("hidden");
  registerSection.classList.remove("hidden");
});

showLogin.addEventListener("click", (e) => {
  e.preventDefault();
  registerSection.classList.add("hidden");
  loginSection.classList.remove("hidden");
});


//  REGISTRAZIONE
document.getElementById("registerBtn").addEventListener("click", async () => {
  const username = document.getElementById("newUsername").value.trim();
  const password = document.getElementById("newPassword").value.trim();
  const confirm = document.getElementById("confirmPassword").value.trim();

  // --- Validazioni ---
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

  // --- Invio al backend ---
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

  } catch (err) {
    msgRegister.textContent = "❌ Errore di connessione al server.";
    msgRegister.style.color = "#d64d4d";
  }
});


// LOGIN
  document.getElementById("loginBtn").addEventListener("click", async () => {
  const username = document.getElementById("username").value.trim();
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

      // Salva solo l'username per l'uso nelle note
      localStorage.setItem("loggedUser", username);

      setTimeout(() => {
        window.location.href = "dashboard.html";
      }, 800);

    } else {
      msg.textContent = `❌ ${data.message || "Credenziali errate."}`;
      msg.style.color = "#d64d4d";
    }

  } catch (err) {
    msg.textContent = "❌ Errore di connessione al server.";
    msg.style.color = "#d64d4d";
  }
});
