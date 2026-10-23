(function () {
    'use strict';
    function initialize() {
        const input = document.getElementById('avatar-input');
        if (!input || !input.dataset.uploadUrl) return;
        const avatars = ['header-avatar', 'hero-avatar'].map(id => document.getElementById(id)).filter(Boolean);
        const status = document.getElementById('avatar-status');
        const toast = document.getElementById('avatar-toast');
        let toastTimer;
        function showMessage(message, failed) {
            if (!toast) return;
            toast.querySelector('.font-label-md').textContent = message;
            const icon = toast.querySelector('.material-symbols-outlined');
            if (icon) icon.textContent = failed ? 'error' : 'check_circle';
            toast.setAttribute('role', failed ? 'alert' : 'status');
            toast.classList.remove('hidden');
            toast.style.transform = 'translateY(0)';
            toast.style.opacity = '1';
            clearTimeout(toastTimer);
            toastTimer = setTimeout(() => toast.classList.add('hidden'), 4000);
        }
        input.addEventListener('change', async () => {
            const file = input.files[0];
            if (!file) return;
            if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type) || file.size > 5 * 1024 * 1024) {
                showMessage('Choose a JPG, PNG, or WEBP image under 5MB.', true);
                input.value = '';
                return;
            }
            const previous = avatars.map(image => image.src);
            const preview = URL.createObjectURL(file);
            avatars.forEach(image => { image.src = preview; });
            status?.classList.remove('hidden');
            input.disabled = true;
            try {
                const body = new FormData();
                body.append('image', file);
                const response = await fetch(input.dataset.uploadUrl, {method: 'POST', body});
                const data = await response.json();
                if (!response.ok || !data.imagePath) throw new Error(data.error || 'Could not upload the photo. Please try again.');
                avatars.forEach(image => { image.src = data.imagePath; });
                window.cityfixPublishProfile?.(null, data.imagePath);
                showMessage(data.message || 'Profile photo updated!', false);
            } catch (error) {
                avatars.forEach((image, index) => { image.src = previous[index]; });
                showMessage(error.message || 'Could not upload the photo. Please try again.', true);
            } finally {
                URL.revokeObjectURL(preview);
                status?.classList.add('hidden');
                input.disabled = false;
                input.value = '';
            }
        });
    }
    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', initialize);
    else initialize();
})();
