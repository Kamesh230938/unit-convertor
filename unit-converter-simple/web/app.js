const state = {
    category: "length",
    catalog: {},
    history: JSON.parse(
        localStorage.getItem("unitConverterHistory") || "[]"
    )
};

const $ = selector => document.querySelector(selector);

const names = {
    length: "Length",
    weight: "Weight",
    temperature: "Temperature",
    area: "Area",
    volume: "Volume",
    speed: "Speed",
    time: "Time"
};

const icons = {
    length: "↔",
    weight: "◈",
    temperature: "°",
    area: "▦",
    volume: "◉",
    speed: "➜",
    time: "◷"
};

window.addEventListener(
    "DOMContentLoaded",
    init
);

async function init() {

    loadTheme();

    $("#inputValue")
        .addEventListener(
            "input",
            debounce(convert, 100)
        );

    $("#fromUnit")
        .addEventListener(
            "change",
            convert
        );

    $("#toUnit")
        .addEventListener(
            "change",
            convert
        );

    $("#swap")
        .addEventListener(
            "click",
            swapUnits
        );

    $("#copy")
        .addEventListener(
            "click",
            copyResult
        );

    $("#clear")
        .addEventListener(
            "click",
            clearHistory
        );

    $("#themeToggle")
        .addEventListener(
            "click",
            toggleTheme
        );

    try {

        const response =
            await fetch("/api/categories");

        if (!response.ok) {
            throw new Error();
        }

        state.catalog =
            await response.json();

        renderCategories();

        selectCategory(
            state.category
        );

        renderHistory();

    } catch {

        $("#status").textContent =
            "Offline";

        showToast(
            "Java server is not running."
        );
    }
}

function renderCategories() {

    const container =
        $("#categories");

    container.innerHTML =
        Object.entries(names)
            .map(
                ([key, name]) => `
                    <button
                        class="category"
                        data-category="${key}"
                    >
                        ${icons[key]}
                        ${name}
                    </button>
                `
            )
            .join("");

    container
        .querySelectorAll(".category")
        .forEach(button => {

            button.addEventListener(
                "click",
                () =>
                    selectCategory(
                        button.dataset.category
                    )
            );
        });
}

function selectCategory(category) {

    if (!state.catalog[category]) {
        return;
    }

    state.category =
        category;

    $("#categoryName").textContent =
        names[category];

    document
        .querySelectorAll(".category")
        .forEach(button => {

            button.classList.toggle(
                "active",
                button.dataset.category === category
            );
        });

    const units =
        state.catalog[category];

    fillSelect(
        $("#fromUnit"),
        units
    );

    fillSelect(
        $("#toUnit"),
        units
    );

    if (category === "temperature") {

        $("#fromUnit").value =
            "celsius";

        $("#toUnit").value =
            "fahrenheit";

    } else {

        $("#fromUnit").value =
            units[0].key;

        $("#toUnit").value =
            units[1]?.key ||
            units[0].key;
    }

    $("#inputValue").value =
        "1";

    convert();
}

function fillSelect(
    select,
    units
) {

    select.innerHTML =
        units
            .map(
                unit =>
                    `<option value="${escapeHtml(unit.key)}">
                        ${escapeHtml(unit.label)}
                    </option>`
            )
            .join("");
}

async function convert() {

    const raw =
        $("#inputValue").value;

    if (
        raw === "" ||
        raw === "-" ||
        raw === "." ||
        raw === "-."
    ) {

        $("#outputValue").value =
            "";

        return;
    }

    const value =
        Number(raw);

    if (!Number.isFinite(value)) {
        return;
    }

    $("#status").textContent =
        "Converting…";

    try {

        const response =
            await fetch(
                "/api/convert",
                {
                    method: "POST",
                    headers: {
                        "Content-Type":
                            "application/json"
                    },
                    body: JSON.stringify({
                        category:
                            state.category,

                        from:
                            $("#fromUnit").value,

                        to:
                            $("#toUnit").value,

                        value
                    })
                }
            );

        const data =
            await response.json();

        if (!response.ok) {
            throw new Error(
                data.error ||
                "Conversion failed"
            );
        }

        $("#outputValue").value =
            data.formattedResult;

        $("#formula").textContent =
            `${formatNumber(value)}
             ${data.fromLabel}
             =
             ${data.formattedResult}
             ${data.toLabel}`;

        $("#status").textContent =
            "Ready";

        addHistory(data);

    } catch {

        $("#status").textContent =
            "Error";
    }
}

function swapUnits() {

    const currentFrom =
        $("#fromUnit").value;

    $("#fromUnit").value =
        $("#toUnit").value;

    $("#toUnit").value =
        currentFrom;

    const result =
        $("#outputValue").value;

    if (result) {

        $("#inputValue").value =
            result;
    }

    convert();
}

async function copyResult() {

    const result =
        $("#outputValue").value;

    if (!result) {
        showToast(
            "Nothing to copy."
        );
        return;
    }

    try {

        await navigator.clipboard
            .writeText(result);

        showToast(
            "Result copied!"
        );

    } catch {

        showToast(
            "Copy failed."
        );
    }
}

function addHistory(data) {

    const item = {
        category: data.category,
        input: data.input,
        result: data.formattedResult,
        from: data.from,
        to: data.to,
        fromLabel: data.fromLabel,
        toLabel: data.toLabel,
        time: Date.now()
    };

    const previous =
        state.history[0];

    if (
        previous &&
        previous.category === item.category &&
        previous.input === item.input &&
        previous.from === item.from &&
        previous.to === item.to &&
        previous.result === item.result
    ) {
        return;
    }

    state.history.unshift(item);

    state.history =
        state.history.slice(0, 8);

    localStorage.setItem(
        "unitConverterHistory",
        JSON.stringify(state.history)
    );

    renderHistory();
}

function renderHistory() {

    const container =
        $("#historyList");

    if (!state.history.length) {

        container.innerHTML =
            `
            <div class="empty">
                Your recent conversions will appear here.
            </div>
            `;

        return;
    }

    container.innerHTML =
        state.history
            .map(
                item => `
                    <div class="history-row">

                        <div class="history-icon">
                            ${icons[item.category] || "↔"}
                        </div>

                        <div class="history-main">

                            <strong>
                                ${formatNumber(item.input)}
                                ${escapeHtml(item.fromLabel)}
                            </strong>

                            <small>
                                ${timeAgo(item.time)}
                            </small>

                        </div>

                        <div class="history-result">

                            ${escapeHtml(item.result)}

                            <small>
                                ${escapeHtml(item.toLabel)}
                            </small>

                        </div>

                    </div>
                `
            )
            .join("");
}

function clearHistory() {

    state.history = [];

    localStorage.removeItem(
        "unitConverterHistory"
    );

    renderHistory();

    showToast(
        "History cleared."
    );
}

function loadTheme() {

    const theme =
        localStorage.getItem(
            "unitConverterTheme"
        );

    const dark =
        theme === "dark" ||
        (
            !theme &&
            window.matchMedia(
                "(prefers-color-scheme: dark)"
            ).matches
        );

    document.documentElement
        .dataset.theme =
        dark ? "dark" : "light";

    $("#themeToggle").textContent =
        dark ? "☀" : "☾";
}

function toggleTheme() {

    const dark =
        document.documentElement
            .dataset.theme !== "dark";

    document.documentElement
        .dataset.theme =
        dark ? "dark" : "light";

    localStorage.setItem(
        "unitConverterTheme",
        dark ? "dark" : "light"
    );

    $("#themeToggle").textContent =
        dark ? "☀" : "☾";
}

function formatNumber(value) {

    return Number(value)
        .toLocaleString(
            "en-US",
            {
                maximumFractionDigits: 12
            }
        );
}

function timeAgo(timestamp) {

    const seconds =
        Math.floor(
            (Date.now() - timestamp) / 1000
        );

    if (seconds < 60) {
        return "Just now";
    }

    const minutes =
        Math.floor(seconds / 60);

    if (minutes < 60) {
        return `${minutes}m ago`;
    }

    const hours =
        Math.floor(minutes / 60);

    if (hours < 24) {
        return `${hours}h ago`;
    }

    return `${Math.floor(hours / 24)}d ago`;
}

function debounce(
    functionToCall,
    delay
) {

    let timer;

    return (...args) => {

        clearTimeout(timer);

        timer =
            setTimeout(
                () => functionToCall(...args),
                delay
            );
    };
}

function escapeHtml(value) {

    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

function showToast(message) {

    const toast =
        $("#toast");

    toast.textContent =
        message;

    toast.classList.add(
        "show"
    );

    clearTimeout(
        showToast.timer
    );

    showToast.timer =
        setTimeout(
            () =>
                toast.classList.remove(
                    "show"
                ),
            2000
        );
}
