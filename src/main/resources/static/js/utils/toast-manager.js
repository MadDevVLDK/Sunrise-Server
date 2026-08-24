const Toast = (() => {
    let container = null;
    let idCounter = 0;

    function getContainer() {
        if (!container) {
            container = document.createElement('div');
            container.id = 'toast-container';
            document.body.appendChild(container);
        }
        return container;
    }

    function show(message, type = 'error', duration = 5000) {
        if (!document.body) return null;

        const toastContainer = getContainer();
        const id = ++idCounter;

        const toast = document.createElement('div');
        toast.className = `sunrise-toast sunrise-toast-${type}`;
        toast.dataset.toastId = id;

        const iconHtml = type === 'loading'
            ? `<div class="sunrise-toast-icon"><div class="sunrise-toast-spinner"></div></div>`
            : `<div class="sunrise-toast-icon"><i class="bi ${getIcon(type)}"></i></div>`;

        toast.innerHTML = `
            ${iconHtml}
            <div class="sunrise-toast-body">${escapeHtml(message)}</div>
            ${type !== 'loading' ? `<button class="sunrise-toast-close" onclick="this.parentElement.remove()">&times;</button>` : ''}
        `;

        toastContainer.appendChild(toast);
        void toast.offsetWidth;
        toast.classList.add('sunrise-toast-visible');

        if (duration > 0 && type !== 'loading') {
            setTimeout(() => {
                dismissToast(toast);
            }, duration);
        }

        const toasts = toastContainer.querySelectorAll('.sunrise-toast');
        if (toasts.length > 5) {
            toasts[0].remove();
        }

        return id;
    }

    function dismissToast(toast) {
        if (!toast || !toast.parentElement) return;
        toast.classList.remove('sunrise-toast-visible');
        toast.classList.add('sunrise-toast-hiding');
        setTimeout(() => {
            if (toast.parentElement) toast.remove();
        }, 300);
    }

    function getIcon(type) {
        const map = {
            error: 'bi-exclamation-circle-fill',
            success: 'bi-check-circle-fill',
            info: 'bi-info-circle-fill',
            warning: 'bi-exclamation-triangle-fill'
        };
        return map[type] || map.info;
    }

    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    return {
        error:   (msg, dur) => show(msg, 'error', dur ?? 5000),
        success: (msg, dur) => show(msg, 'success', dur ?? 3000),
        info:    (msg, dur) => show(msg, 'info', dur ?? 4000),
        warning: (msg, dur) => show(msg, 'warning', dur ?? 5000),

        /** Показывает тост с лоадером. Возвращает ID для закрытия. */
        loading: (msg) => show(msg, 'loading', 0),

        /** Закрывает тост по ID (для loading). */
        dismiss: (id) => {
            const toast = document.querySelector(`[data-toast-id="${id}"]`);
            if (toast) dismissToast(toast);
        }
    };
})();