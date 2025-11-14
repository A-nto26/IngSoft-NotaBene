// ===== SWITCH LOGIN / REGISTER =====
document.getElementById("showRegister").addEventListener("click", () => {
  document.getElementById("loginSection").classList.add("hidden");
  document.getElementById("registerSection").classList.remove("hidden");
});

document.getElementById("showLogin").addEventListener("click", () => {
  document.getElementById("registerSection").classList.add("hidden");
  document.getElementById("loginSection").classList.remove("hidden");
});

// ===== LOGIN =====
document.getElementById("loginBtn").addEventListener("click", async () => {
  const username = document.getElementById("username").value.trim();
  const password = document.getElementById("password").value.trim();

  const res = await fetch(
    `http://localhost:8080/api/users/login?username=${username}&password=${password}`,
    { method: "POST" }
  );

  const text = await res.text();

  if (res.ok) {
    localStorage.setItem("loggedUser", username);
    window.location.href = "dashboard.html";
  } else {
    document.getElementById("msg").textContent = text;
  }
});

// ===== REGISTER =====
document.getElementById("registerBtn").addEventListener("click", async () => {
  const username = document.getElementById("newUsername").value.trim();
  const password = document.getElementById("newPassword").value.trim();
  const confirm = document.getElementById("confirmPassword").value.trim();

  if (password !== confirm) {
    document.getElementById("msgRegister").textContent = "Le password non coincidono.";
    return;
  }

  const res = await fetch(
    `http://localhost:8080/api/users/register?username=${username}&password=${password}`,
    { method: "POST" }
  );

  const text = await res.text();
  document.getElementById("msgRegister").textContent = text;
});
