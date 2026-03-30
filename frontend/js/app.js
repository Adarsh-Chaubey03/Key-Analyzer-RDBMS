(function () {
    'use strict';

    const API_URL = '/api/compute-keys';

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

    computeBtn.addEventListener('click', handleCompute);

    document.addEventListener('keydown', function (event) {
        if ((event.ctrlKey || event.metaKey) && event.key === 'Enter') {
            handleCompute();
        }
    });

    async function handleCompute() {
        hideError();

        const attributes = parseAttributes(attrInput.value);
        if (attributes.length === 0) {
            showError('Please enter at least one attribute.');
            return;
        }

        const fds = parseFDs(fdsInput.value, attributes);
        if (fds === null) {
            return;
        }
        if (fds.length === 0) {
            showError('Please enter at least one functional dependency.');
            return;
        }

        const attributeSet = new Set(attributes);
        for (const fd of fds) {
            for (const attribute of [...fd.left, ...fd.right]) {
                if (!attributeSet.has(attribute)) {
                    showError(`Attribute "${attribute}" used in an FD is not in the relation.`);
                    return;
                }
            }
        }

        setLoading(true);

        try {
            const response = await fetch(API_URL, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ attributes, fds }),
            });

            const data = await readJson(response);
            if (!response.ok) {
                showError(data.error || `Server error (${response.status})`);
                return;
            }

            renderResults(data);
        } catch (error) {
            showError('Failed to connect to the server. Is the backend running?');
        } finally {
            setLoading(false);
        }
    }

    async function readJson(response) {
        try {
            return await response.json();
        } catch (error) {
            return {};
        }
    }

    function parseAttributes(text) {
        const trimmed = text.trim();
        if (!trimmed) {
            return [];
        }

        const rawAttributes = trimmed.includes(',')
            ? trimmed.split(',')
            : trimmed.split(/\s+/);

        return unique(rawAttributes.map(normalize).filter(Boolean));
    }

    function parseFDs(text, attributes) {
        const lines = text.split('\n').map(normalize).filter(Boolean);
        const knownAttributes = new Set(attributes);
        const fds = [];

        for (const line of lines) {
            const parts = line.split(/->|\u2192/);
            if (parts.length !== 2) {
                showError(`Invalid FD format: "${line}". Use "X -> Y".`);
                return null;
            }

            const left = splitAttributes(parts[0], knownAttributes);
            const right = splitAttributes(parts[1], knownAttributes);

            if (left.length === 0 || right.length === 0) {
                showError(`Empty side in FD: "${line}".`);
                return null;
            }

            fds.push({ left, right });
        }

        return fds;
    }

    function splitAttributes(value, knownAttributes) {
        const trimmed = normalize(value);
        if (!trimmed) {
            return [];
        }

        if (trimmed.includes(',')) {
            return unique(trimmed.split(',').map(normalize).filter(Boolean));
        }

        const tokens = trimmed.split(/\s+/).filter(Boolean);
        if (tokens.length > 1) {
            return unique(tokens);
        }

        const token = tokens[0];
        if (knownAttributes.has(token)) {
            return [token];
        }

        const characters = token.split('');
        if (characters.every(function (attribute) { return knownAttributes.has(attribute); })) {
            return unique(characters);
        }

        return [token];
    }

    function renderResults(data) {
        placeholder.hidden = true;
        resultsContainer.hidden = false;

        renderKeySection(candidateKeysEl, data.candidateKeys, 'candidate', 'No candidate keys were returned.');

        superkeysEl.innerHTML = '';
        if (typeof data.superkeys === 'string') {
            const message = document.createElement('div');
            message.className = 'superkey-message';
            message.textContent = data.superkeys;
            superkeysEl.appendChild(message);
        } else {
            renderKeySection(superkeysEl, data.superkeys, 'superkey', 'No superkeys were returned.');
        }

        infoPanel.innerHTML = '';
        if (data.info) {
            const items = [
                { label: 'Attributes', value: data.info.attributeCount },
                { label: 'Candidate Keys', value: data.info.candidateKeyCount },
                { label: 'Superkeys', value: data.info.superkeysEnumerated ? 'Enumerated' : 'Skipped' },
            ];

            items.forEach(function (item) {
                const card = document.createElement('div');
                card.className = 'info-item';
                card.innerHTML =
                    `<div class="info-label">${item.label}</div>` +
                    `<div class="info-value">${item.value}</div>`;
                infoPanel.appendChild(card);
            });
        }
    }

    function renderKeySection(container, keys, type, emptyMessage) {
        container.innerHTML = '';

        if (!Array.isArray(keys) || keys.length === 0) {
            const emptyState = document.createElement('div');
            emptyState.className = 'empty-state';
            emptyState.textContent = emptyMessage;
            container.appendChild(emptyState);
            return;
        }

        keys.forEach(function (key, index) {
            const badge = document.createElement('span');
            badge.className = `key-badge ${type}`;
            badge.style.animationDelay = `${index * 0.04}s`;
            badge.innerHTML =
                '<span class="brace">{</span>' +
                key.join(', ') +
                '<span class="brace">}</span>';
            container.appendChild(badge);
        });
    }

    function setLoading(isLoading) {
        computeBtn.disabled = isLoading;
        btnText.hidden = isLoading;
        btnLoader.hidden = !isLoading;
    }

    function showError(message) {
        errorMsg.textContent = message;
        errorMsg.hidden = false;
    }

    function hideError() {
        errorMsg.hidden = true;
        errorMsg.textContent = '';
    }

    function unique(values) {
        return Array.from(new Set(values));
    }

    function normalize(value) {
        return value.trim();
    }
})();
