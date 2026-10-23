(function () {
    'use strict';

    const profileFields = {
        'field-name': 'name', 'field-phone': 'phone', 'field-city': 'city',
        'field-zip': 'zipCode', 'field-address': 'street', 'field-department': 'department',
        'pw-current': 'currentPassword', 'pw-new': 'password', 'pw-confirm': 'confirmPassword'
    };
    const bounds = {
        name: [2, 100], street: [1, 200], city: [1, 100], department: [1, 100],
        title: [5, 150], description: [10, 1000], location: [1, 255]
    };
    const keyOf = field => (field.name || profileFields[field.id] || '').split('.').pop();
    const phone = value => value.replace(/[\s()\-]/g, '').replace(/^\+?88(?=01)/, '');

    function prepare(form) {
        Array.from(form.elements).forEach(field => {
            const key = keyOf(field);
            if (bounds[key]) {
                field.required = true;
                field.minLength = bounds[key][0];
                field.maxLength = bounds[key][1];
            }
            if (key === 'phone') {
                field.required = true;
                field.pattern = '01[3-9][0-9]{8}';
                field.maxLength = 20;
                field.title = 'Enter a valid mobile number, e.g. 01712345678.';
            }
            if (key === 'zipCode') {
                field.required = true;
                field.pattern = '[0-9]{4}';
                field.maxLength = 4;
                field.inputMode = 'numeric';
                field.title = 'Enter a valid 4-digit postal code.';
            }
            if (key === 'email') { field.required = true; field.maxLength = 254; }
            if (key === 'password' && !new URL(form.action || location.href).pathname.endsWith('/login')) {
                field.required = true; field.minLength = 6; field.maxLength = 72;
            }
            if (key === 'currentPassword' || key === 'confirmPassword' || key === 'position' || key === 'gender') field.required = true;
            if (key === 'costNotes') field.maxLength = 1000;
            if (key === 'finalCost') { field.min = '0'; field.step = '0.01'; }
        });
    }

    function validate(form) {
        const password = Array.from(form.elements).find(field => keyOf(field) === 'password');
        Array.from(form.elements).forEach(field => {
            if (!field.willValidate || typeof field.setCustomValidity !== 'function') return;
            const key = keyOf(field);
            field.setCustomValidity('');
            if (key === 'phone') field.value = phone(field.value);
            if (bounds[key] || key === 'email' || key === 'zipCode') field.value = field.value.trim();
            if (bounds[key] && field.value && (field.value.length < bounds[key][0] || field.value.length > bounds[key][1])) {
                field.setCustomValidity('Please enter between ' + bounds[key][0] + ' and ' + bounds[key][1] + ' characters.');
            }
            if (key === 'password' && field.minLength === 6 && field.value && (field.value.length < 6 || field.value.length > 72)) {
                field.setCustomValidity('Password must be between 6 and 72 characters.');
            }
            if (key === 'phone' && field.value && !/^01[3-9][0-9]{8}$/.test(field.value)) {
                field.setCustomValidity('Enter a valid mobile number, e.g. 01712345678.');
            }
            if (key === 'zipCode' && field.value && !/^[0-9]{4}$/.test(field.value)) {
                field.setCustomValidity('Enter a valid 4-digit postal code.');
            }
            if (key === 'confirmPassword' && password && field.value !== password.value) {
                field.setCustomValidity('Passwords do not match.');
            }
            field.setAttribute('aria-invalid', String(!field.validity.valid));
        });
        return form.checkValidity();
    }

    function initialize() {
        document.querySelectorAll('form').forEach(form => {
            prepare(form);
            // Capture submit first so invalid AJAX forms never reach their fetch handlers.
            form.noValidate = true;
            form.addEventListener('input', () => validate(form));
        });
        document.addEventListener('submit', event => {
            const form = event.target;
            if (form.tagName !== 'FORM' || validate(form)) return;
            event.preventDefault();
            event.stopImmediatePropagation();
            form.reportValidity();
        }, true);
    }

    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', initialize);
    else initialize();
})();
