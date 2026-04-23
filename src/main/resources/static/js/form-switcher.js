/**
 * Переключить на указанную форму
 * @param {string} formId - ID формы для отображения (loginForm, registerForm, forgotPasswordForm)
 */
function switchForm(formId) {
    // Получить все панели форм
    const formPanels = document.querySelectorAll('.form-panel');

    // Скрыть все панели
    formPanels.forEach(panel => {
        panel.classList.remove('active');
    });

    // Показать указанную панель
    const targetForm = document.getElementById(formId);
    if (targetForm) {
        targetForm.classList.add('active');

        // Скролл к форме на мобильных устройствах
        if (window.innerWidth < 768) {
            targetForm.scrollIntoView({ behavior: 'smooth' });
        }

        // Очистить сообщения об ошибках при переключении
        const messageElements = targetForm.querySelectorAll('.alert-message');
        messageElements.forEach(el => {
            el.style.display = 'none';
            el.textContent = '';
            el.className = 'alert-message';
        });

        // Сфокусировать первый input
        const firstInput = targetForm.querySelector('input');
        if (firstInput) {
            setTimeout(() => firstInput.focus(), 100);
        }
    }
}

