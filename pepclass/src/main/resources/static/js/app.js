document.addEventListener('DOMContentLoaded', () => {
    const roleButtons = document.querySelectorAll('.role-option');
    const hiddenRole = document.querySelector('#loginRole');
    const submitButton = document.querySelector('.auth-card button[type="submit"]');

    if (!roleButtons.length || !hiddenRole || !submitButton) {
        return;
    }

    const labels = {
        chairman: 'Continue as Chairman',
        doctor: 'Continue as Doctor',
        patient: 'Continue as Patient'
    };

    roleButtons.forEach((button) => {
        button.addEventListener('click', () => {
            roleButtons.forEach((item) => item.classList.remove('active'));
            button.classList.add('active');
            hiddenRole.value = button.dataset.role;
            submitButton.textContent = labels[button.dataset.role];
        });
    });
});
