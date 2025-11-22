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

// ===============================
//     CREA NOTA
// ===============================
document.getElementById("createNoteBtn").addEventListener("click", async () => {
  const titolo = document.getElementById("titolo").value.trim();
  const contenuto = document.getElementById("contenuto").value.trim();
  const cartella = document.getElementById("cartella")?.value.trim() || "";

  if (!titolo || !contenuto) {
    alert("Compila tutti i campi.");
    return;
  }

  await fetch(
    `${API}?titolo=${encodeURIComponent(titolo)}&contenuto=${encodeURIComponent(contenuto)}&creatore=${user}&cartella=${encodeURIComponent(cartella)}`,
    { method: "POST" }
  );

  loadNotes();

  document.getElementById("titolo").value = "";
  document.getElementById("contenuto").value = "";
  if (document.getElementById("cartella")) document.getElementById("cartella").value = "";
});

// ===============================
//     CARICA NOTE
// ===============================
async function loadNotes() {
  const res = await fetch(`${API}/${user}`);
  const notes = await res.json();

  const list = document.getElementById("noteList");
  list.innerHTML = "";

  notes.forEach(n => {
    const li = document.createElement("li");

    li.innerHTML = `
      <strong>${n.titolo}</strong><br>
      <em>${n.contenuto}</em><br>
      Cartella: ${n.cartella || "—"}<br>
      <button onclick="deleteNote(${n.id})">🗑 Elimina</button>
      <button onclick="duplicateNote(${n.id})">📝 Duplica</button>
      <button onclick="editNote(${n.id})">✏ Modifica</button>
    `;

    list.appendChild(li);
  });
}

loadNotes();

// ===============================
//     ELIMINA NOTA
// ===============================
async function deleteNote(id) {
  if (!confirm("Vuoi davvero eliminare questa nota?")) return;

  await fetch(`${API}/${id}`, { method: "DELETE" });
  loadNotes();
}

// ===============================
//     DUPLICA NOTA
// ===============================
async function duplicateNote(id) {
  await fetch(`${API}/${id}/duplicate?creatore=${user}`, {
    method: "POST"
  });

  loadNotes();
}

// ===============================
//     MODIFICA NOTA
// ===============================
async function editNote(id) {
  const nuovoTitolo = prompt("Nuovo titolo:");
  const nuovoContenuto = prompt("Nuovo contenuto:");

  if (!nuovoTitolo && !nuovoContenuto) return;

  const params = new URLSearchParams();
  if (nuovoTitolo) params.append("titolo", nuovoTitolo);
  if (nuovoContenuto) params.append("contenuto", nuovoContenuto);

  await fetch(`${API}/${id}?${params.toString()}`, {
    method: "PUT"
  });

  loadNotes();
}

// ===============================
//     RICERCA NOTE
// ===============================
document.getElementById("searchBtn")?.addEventListener("click", async () => {
  const query = document.getElementById("searchInput").value.trim();

  const res = await fetch(`${API}/search?user=${user}&q=${encodeURIComponent(query)}`);
  const notes = await res.json();

  const list = document.getElementById("noteList");
  list.innerHTML = "";

  notes.forEach(n => {
    const li = document.createElement("li");

    li.innerHTML = `
      <strong>${n.titolo}</strong><br>
      <em>${n.contenuto}</em><br>
      Cartella: ${n.cartella || "—"}<br>
      <button onclick="deleteNote(${n.id})">🗑 Elimina</button>
      <button onclick="duplicateNote(${n.id})">📝 Duplica</button>
      <button onclick="editNote(${n.id})">✏ Modifica</button>
    `;

    list.appendChild(li);
  });
});

// ===============================
//     ASSEGNA CARTELLA
// ===============================
async function setFolder(id) {
  const cartella = prompt("Inserisci nome cartella:");
  if (!cartella) return;

  await fetch(`${API}/${id}/folder?cartella=${encodeURIComponent(cartella)}`, {
    method: "PUT"
  });

  loadNotes();
}
