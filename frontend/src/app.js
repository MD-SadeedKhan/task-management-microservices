// Task Management Console
// Talks only to the gateway, via relative paths that nginx proxies to it
// (see nginx.conf.template). No direct calls to user-service/task-service.

const STATUS_META = {
  TODO: { label: "To do", className: "pill--todo" },
  IN_PROGRESS: { label: "In progress", className: "pill--in-progress" },
  DONE: { label: "Done", className: "pill--done" },
};

function statusPill(status) {
  const meta = STATUS_META[status] || { label: status, className: "pill--todo" };
  return `<span class="pill ${meta.className}"><span class="pill__dot"></span>${meta.label}</span>`;
}

async function apiRequest(path, options) {
  const response = await fetch(path, options);
  if (response.status === 204) {
    return null;
  }
  let body = null;
  try {
    body = await response.json();
  } catch (_) {
    // no JSON body (e.g. some error responses) - fine
  }
  if (!response.ok) {
    const message = (body && body.message) || `Request failed with status ${response.status}`;
    throw new Error(message);
  }
  return body;
}

function showError(elementId, message) {
  const el = document.getElementById(elementId);
  el.textContent = message;
  el.hidden = false;
}

function clearError(elementId) {
  const el = document.getElementById(elementId);
  el.hidden = true;
  el.textContent = "";
}

/* ---------------------------------------------------------------
   Gateway status indicator
------------------------------------------------------------------ */
async function refreshGatewayStatus() {
  const dot = document.querySelector("#gateway-status .status__dot");
  const label = document.querySelector("#gateway-status .status__label");
  try {
    const res = await fetch("/actuator/health/readiness");
    if (res.ok) {
      dot.dataset.state = "up";
      label.textContent = "Gateway online";
    } else {
      dot.dataset.state = "down";
      label.textContent = "Gateway unavailable";
    }
  } catch (_) {
    dot.dataset.state = "down";
    label.textContent = "Gateway unreachable";
  }
}

/* ---------------------------------------------------------------
   Users
------------------------------------------------------------------ */
async function loadUsers() {
  const list = document.getElementById("user-list");
  const count = document.getElementById("users-count");
  try {
    const users = await apiRequest("/users");
    count.textContent = users.length;

    if (users.length === 0) {
      list.innerHTML = `<li class="list__empty">No users yet — add one above to get started.</li>`;
      return;
    }

    list.innerHTML = users
      .map(
        (user) => `
        <li class="list__item" data-id="${user.id}">
          <div class="list__item-main">
            <span class="list__item-title">${escapeHtml(user.name)}</span>
            <span class="list__item-meta">#${user.id} · ${escapeHtml(user.email)}</span>
          </div>
          <button class="btn btn--danger" data-action="delete-user" data-id="${user.id}">Delete</button>
        </li>`
      )
      .join("");
  } catch (err) {
    showError("user-error", `Couldn't load users: ${err.message}`);
    list.innerHTML = `<li class="list__empty">Unable to load users.</li>`;
  }
}

async function createUser(event) {
  event.preventDefault();
  clearError("user-error");
  const form = event.target;
  const payload = {
    name: form.name.value.trim(),
    email: form.email.value.trim(),
  };
  try {
    await apiRequest("/users", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });
    form.reset();
    await loadUsers();
  } catch (err) {
    showError("user-error", `Couldn't add user: ${err.message}`);
  }
}

async function deleteUser(id) {
  clearError("user-error");
  try {
    await apiRequest(`/users/${id}`, { method: "DELETE" });
    await loadUsers();
  } catch (err) {
    showError("user-error", `Couldn't delete user: ${err.message}`);
  }
}

/* ---------------------------------------------------------------
   Tasks
------------------------------------------------------------------ */
async function loadTasks() {
  const list = document.getElementById("task-list");
  const count = document.getElementById("tasks-count");
  try {
    const tasks = await apiRequest("/tasks");
    count.textContent = tasks.length;

    if (tasks.length === 0) {
      list.innerHTML = `<li class="list__empty">No tasks yet — add one above to get started.</li>`;
      return;
    }

    list.innerHTML = tasks
      .map(
        (task) => `
        <li class="list__item" data-id="${task.id}">
          <div class="list__item-main">
            <span class="list__item-title">${escapeHtml(task.title)}</span>
            <span class="list__item-meta">#${task.id} · user #${task.userId}${task.description ? " · " + escapeHtml(task.description) : ""}</span>
          </div>
          ${statusPill(task.status)}
          <button class="btn btn--danger" data-action="delete-task" data-id="${task.id}">Delete</button>
        </li>`
      )
      .join("");
  } catch (err) {
    showError("task-error", `Couldn't load tasks: ${err.message}`);
    list.innerHTML = `<li class="list__empty">Unable to load tasks.</li>`;
  }
}

async function createTask(event) {
  event.preventDefault();
  clearError("task-error");
  const form = event.target;
  const payload = {
    title: form.title.value.trim(),
    description: form.description.value.trim() || null,
    status: form.status.value,
    userId: Number(form.userId.value),
  };
  try {
    await apiRequest("/tasks", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });
    form.reset();
    await loadTasks();
  } catch (err) {
    showError("task-error", `Couldn't add task: ${err.message}`);
  }
}

async function deleteTask(id) {
  clearError("task-error");
  try {
    await apiRequest(`/tasks/${id}`, { method: "DELETE" });
    await loadTasks();
  } catch (err) {
    showError("task-error", `Couldn't delete task: ${err.message}`);
  }
}

/* ---------------------------------------------------------------
   Helpers + wiring
------------------------------------------------------------------ */
function escapeHtml(value) {
  const div = document.createElement("div");
  div.textContent = value ?? "";
  return div.innerHTML;
}

document.getElementById("user-form").addEventListener("submit", createUser);
document.getElementById("task-form").addEventListener("submit", createTask);

document.getElementById("user-list").addEventListener("click", (event) => {
  const button = event.target.closest('[data-action="delete-user"]');
  if (button) {
    deleteUser(button.dataset.id);
  }
});

document.getElementById("task-list").addEventListener("click", (event) => {
  const button = event.target.closest('[data-action="delete-task"]');
  if (button) {
    deleteTask(button.dataset.id);
  }
});

loadUsers();
loadTasks();
refreshGatewayStatus();
setInterval(refreshGatewayStatus, 15000);
