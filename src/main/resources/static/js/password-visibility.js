// Функция для переключения видимости пароля
function togglePasswordVisibility(button) {
    // Найти input в родительском контейнере
    const inputWrapper = button.closest('.input-wrapper');
    const input = inputWrapper.querySelector('input[type="password"], input[type="text"]');
    const icon = button.querySelector('i');

    if (!input) return;

    if (input.type === 'password') {
        input.type = 'text';
        icon.classList.remove('bi-eye-fill');
        icon.classList.add('bi-eye-slash-fill');
    } else {
        input.type = 'password';
        icon.classList.remove('bi-eye-slash-fill');
        icon.classList.add('bi-eye-fill');
    }
}