/**
 * DBMS Key Analyzer — Frontend Logic
 * Parses user input, calls the backend API, and renders results.
 */
(function () {
    'use strict';

    const API_URL = '/api/compute-keys';

    // DOM refs
    const attrInput = document.getElementById('attributes-input');
    const fdsInput = document.getElementById('fds-input');
    const computeBtn = document.getElementById('compute-btn');
    const btnText = computeBtn.querySelector('.btn-text');
    const btnLoader = computeBtn.querySelector('.btn-loader');
    const errorMsg = document.getElementById('error-msg');
    const placeholder = document.getElementById('output-placeholder');
    const resultsContainer = document.getElementById('results-container');
    const candidateKeysEl = document.getElementById('candidate-keys');
    const superkeysEl = document.getElementById('superkeys');
    const infoPanel = document.getElementById('info-panel');

    // --- Event Listeners ---
    computeBtn.addEventListener('click', handleCompute);

    // Allow Ctrl+Enter to submit
    document.addEventListener('keydown', function (e) {
        if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
            handleCompute();
        }
    });

    // --- Main Handler ---
    async function handleCompute() {
        hideError();

        const attributes = parseAttributes(attrInput.value);
        const fds = parseFDs(fdsInput.value);

        // Validate
        if (attributes.length === 0) {
            showError('Please enter at least one attribute.');
            return;
        }
        if (fds === null) {
            return; // parseFDs already showed an error
        }
        if (fds.length === 0) {
            showError('Please enter at least one functional dependency.');
            return;
        }

        // Validate FD attributes against declared attributes
        const attrSet = new Set(attributes);
        for (const fd of fds) {
            for (const a of [...fd.left, ...fd.right]) {
                if (!attrSet.has(a)) {
                    showError(`Attribute "${a}" used in FD is not in the relation.`);
                    return;
                }
            }
        }

        const payload = { attributes, fds };

        setLoading(true);

        try {
            const resp = await fetch(API_URL, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload),
            });

            const data = await resp.json();

            if (!resp.ok) {
                showError(data.error || `Server error (${resp.status})`);
                return;
            }

            renderResults(data);
        } catch (err) {
            showError('Failed to connect to the server. Is the backend running?');
        } finally {
            setLoading(false);
        }
    }

    // --- Parsers ---

    function parseAttributes(text) {
        return text
            .split(/[,\s]+/)
            .map(s => s.trim())
            .filter(s => s.length > 0);
    }

    /**
     * Parse multi-line FD text into structured objects.
     * Supports: A -> B, AB -> CD, A,B -> C,D
     * Separator: -> or →
     */
    function parseFDs(text) {
        const lines = text.split('\n').map(l => l.trim()).filter(l => l.length > 0);
        const fds = [];

        for (const line of lines) {
            // Split on -> or →
            const parts = line.split(/->|→/);
            if (parts.length !== 2) {
                showError(`Invalid FD format: "${line}". Use "X -> Y".`);
                return null;
            }

            const left = splitAttrs(parts[0]);
            const right = splitAttrs(parts[1]);

            if (left.length === 0 || right.length === 0) {
                showError(`Empty side in FD: "${line}".`);
                return null;
            }

            fds.push({ left, right });
        }

        return fds;
    }

    /**
     * Split attribute string: handles both "AB" (single-char each) and "A,B" (comma-separated).
     * Heuristic: if there are commas, use comma split; otherwise treat each char as an attribute.
     */
    function splitAttrs(str) {
        str = str.trim();
        if (str.includes(',')) {
            return str.split(',').map(s => s.trim()).filter(s => s.length > 0);
        }
        // If any token is multi-char without commas... check if it looks like single chars
        const tokens = str.split(/\s+/).filter(s => s.length > 0);
        if (tokens.length === 1 && tokens[0].length > 1) {
            // Treat each character as a separate attribute (e.g., "AB" → ["A","B"])
            return tokens[0].split('');
        }
        return tokens;
    }

    // --- Rendering ---

    function renderResults(data) {
        placeholder.hidden = true;
        resultsContainer.hidden = false;

        // Candidate Keys
        candidateKeysEl.innerHTML = '';
        data.candidateKeys.forEach((key, i) => {
            const badge = createKeyBadge(key, 'candidate', i);
            candidateKeysEl.appendChild(badge);
        });

        // Superkeys
        superkeysEl.innerHTML = '';
        if (typeof data.superkeys === 'string') {
            const msg = document.createElement('div');
            msg.className = 'superkey-message';
            msg.textContent = data.superkeys;
            superkeysEl.appendChild(msg);
        } else {
            data.superkeys.forEach((key, i) => {
                const badge = createKeyBadge(key, 'superkey', i);
                superkeysEl.appendChild(badge);
            });
        }

        // Info
        infoPanel.innerHTML = '';
        if (data.info) {
            const items = [
                { label: 'Attributes', value: data.info.attributeCount },
                { label: 'Candidate Keys', value: data.info.candidateKeyCount },
            ];
            items.forEach(item => {
                const div = document.createElement('div');
                div.className = 'info-item';
                div.innerHTML = `
                    <div class="info-label">${item.label}</div>
                    <div class="info-value">${item.value}</div>
                `;
                infoPanel.appendChild(div);
            });
        }
    }

    function createKeyBadge(keyAttrs, type, index) {
        const badge = document.createElement('span');
        badge.className = `key-badge ${type}`;
        badge.style.animationDelay = `${index * 0.04}s`;
        badge.innerHTML =
            '<span class="brace">{</span>' +
            keyAttrs.join(', ') +
            '<span class="brace">}</span>';
        return badge;
    }

    // --- Helpers ---

    function setLoading(on) {
        computeBtn.disabled = on;
        btnText.hidden = on;
        btnLoader.hidden = !on;
    }

    function showError(msg) {
        errorMsg.textContent = msg;
        errorMsg.hidden = false;
    }

    function hideError() {
        errorMsg.hidden = true;
        errorMsg.textContent = '';
    }
})();
