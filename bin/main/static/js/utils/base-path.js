/**
 * Получает базовый путь приложения на основе текущего URL
 * Это необходимо для правильной работы когда приложение установлено на подпути (например /app)
 * @returns {string} Базовый путь без завершающего слеша
 */
function getBasePath() {
    const pathname = window.location.pathname;
    if (pathname === '/' || pathname === '') {
        return '';
    }

    // Берем первый сегмент пути (например из "/app/static/js/auth-form-handler.js" получаем "/app")
    // Но нужно быть аккуратнее - если это /static/js, то это не базовый путь

    // Более надежный способ - получить базовый путь из meta тега или из первого сегмента path
    // который не является "/static", "/api" и т.д.

    const segments = pathname.split('/').filter(s => s.length > 0);

    // Если первый сегмент это не служебная папка, то это базовый путь
    if (segments.length > 0 && !['static', 'api', 'forms'].includes(segments[0])) {
        return '/' + segments[0];
    }

    return '';
}

function getApiPath(path) {
    const basePath = getBasePath();
    if (basePath) {
        return basePath + '/api' + path;
    }
    return '/api' + path;
}

function getFormsPath(path) {
    const basePath = getBasePath();
    if (basePath) {
        return basePath + '/forms' + path;
    }
    return '/forms' + path;
}

console.log('[BasePath] Initialized. Current base path:', getBasePath());