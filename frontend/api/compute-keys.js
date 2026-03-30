module.exports = async function handler(request, response) {
    if (request.method === 'OPTIONS') {
        response.setHeader('Allow', 'OPTIONS, POST');
        return response.status(204).end();
    }

    if (request.method !== 'POST') {
        response.setHeader('Allow', 'OPTIONS, POST');
        return response.status(405).json({ error: 'Method not allowed.' });
    }

    const backendApiUrl = process.env.BACKEND_API_URL;
    if (!backendApiUrl) {
        return response.status(500).json({ error: 'BACKEND_API_URL is not configured.' });
    }

    const upstreamUrl = buildComputeKeysUrl(backendApiUrl);
    const requestBody = typeof request.body === 'string'
        ? request.body
        : JSON.stringify(request.body ?? {});

    try {
        const upstreamResponse = await fetch(upstreamUrl, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: requestBody,
        });

        const responseBody = await upstreamResponse.text();
        const contentType = upstreamResponse.headers.get('content-type') || 'application/json; charset=utf-8';

        response.status(upstreamResponse.status);
        response.setHeader('Content-Type', contentType);
        response.setHeader('Cache-Control', 'no-store');
        return response.send(responseBody);
    } catch (error) {
        return response.status(502).json({ error: 'Unable to reach the backend API.' });
    }
};

function buildComputeKeysUrl(baseUrl) {
    const trimmedBaseUrl = baseUrl.replace(/\/+$/, '');
    return trimmedBaseUrl.endsWith('/api/compute-keys')
        ? trimmedBaseUrl
        : `${trimmedBaseUrl}/api/compute-keys`;
}
