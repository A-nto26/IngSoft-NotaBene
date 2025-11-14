const API = "http://localhost:8080/api/notes";
const user = localStorage.getItem("loggedUser");

// ===== BLOCCO ACCESSO DIRETTO =====
if (!user) {
  alert("Devi prima effettuare il login.");
  window.location.href = "auth.html";
}

// ===== MOSTRA USER =====
document.getElementById("welcomeUser").textContent = "Ciao, " + user;

// ===== LOGOUT =====
document.getElementById("logoutBtn").addEventListener("click", () => {
  localStorage.removeItem("loggedUser");
  window.location.href = "auth.html";
});

// ===== CREA NOTA =====
document.getElementById("createNoteBtn").addEventListener("click", async () => {
  const titolo = document.getElementById("titolo").value.trim();
  const contenuto = document.getElementById("contenuto").value.trim();

  if (!titolo || !contenuto) {
    alert("Compila tutti i campi.");
    return;
  }

  await fetch(
    `${API}?titolo=${encodeURIComponent(titolo)}&contenuto=${encodeURIComponent(contenuto)}&creatore=${user}`,
    { method: "POST" }
  );

  loadNotes();

  document.getElementById("titolo").value = "";
  document.getElementById("contenuto").value = "";
});

// ===== CARICA NOTE =====
async function loadNotes() {
  const res = await fetch(`${API}/${user}`);
  const notes = await res.json();

  const list = document.getElementById("noteList");
  list.innerHTML = "";

  notes.forEach(n => {
    const li = document.createElement("li");
    li.textContent = `${n.titolo} — ${n.contenuto}`;
    list.appendChild(li);
  });
}

loadNotes();
