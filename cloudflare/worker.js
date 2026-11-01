async function rewriteMetadata(response, requestUrl) {
    const contentType = response.headers.get("content-type") ?? "";

    if (!contentType.includes("text/html")) {
        return response;
    }

    return new HTMLRewriter()
        .on('meta[property="og:image"]', {
            element(element) {
                const content = element.getAttribute("content");
                if (!content) return;

                let image;
                try {
                    image = new URL(content, requestUrl);
                } catch {
                    return;
                }
                image.host = requestUrl.host;
                image.protocol = requestUrl.protocol;
                element.setAttribute("content", image.toString());
            }
        })
        .transform(response);
}

export default {
    async fetch(request, env) {
        const requestUrl = new URL(request.url);

        try {
            /*if (requestUrl.pathname === "/test-error") {
                throw new Error("Test internal server error");
            }*/

            let response = await env.ASSETS.fetch(request);

            if (response.status >= 500) {
                response = await env.ASSETS.fetch(
                    new URL("/500", requestUrl)
                );

                response = new Response(response.body, {
                    status: 500,
                    headers: response.headers
                });
            }

            return await rewriteMetadata(response, requestUrl);
        } catch {
            try {
                const response = await env.ASSETS.fetch(
                    new URL("/500", requestUrl)
                );

                return await rewriteMetadata(
                    new Response(response.body, {
                        status: 500,
                        headers: response.headers
                    }),
                    requestUrl
                );
            } catch {
                return new Response("Internal Server Error", { status: 500 });
            }
        }
    }
};
