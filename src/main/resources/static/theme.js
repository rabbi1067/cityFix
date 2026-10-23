/* CityFix shared day/night toggle.
   1) Runs immediately (no DOMContentLoaded wait) so the saved theme is
      applied before first paint -> no light/dark flash.
   2) Wires up every button with class="theme-toggle" on the page. */
(function () {
    var STORAGE_KEY = 'cityfix-theme';
    var saved = localStorage.getItem(STORAGE_KEY);
    var theme = saved === 'dark' ? 'dark' : 'light';
    document.documentElement.setAttribute('data-theme', theme);

    function wireToggles() {
        document.querySelectorAll('.theme-toggle').forEach(function (btn) {
            if (btn.dataset.themeWired) return;
            btn.dataset.themeWired = '1';
            btn.addEventListener('click', function () {
                var current = document.documentElement.getAttribute('data-theme');
                var next = current === 'dark' ? 'light' : 'dark';
                document.documentElement.setAttribute('data-theme', next);
                localStorage.setItem(STORAGE_KEY, next);
            });
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', wireToggles);
    } else {
        wireToggles();
    }
})();
