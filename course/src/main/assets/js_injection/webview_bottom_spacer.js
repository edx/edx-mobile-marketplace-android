(function() {
    if (!document.getElementById('bottom-padding')) {
        const spacer = document.createElement('div');
        spacer.id = 'bottom-padding';
        Object.assign(spacer.style, {
            height: '100px',
            width: '100%',
            background: 'transparent'
        });
        document.body.appendChild(spacer);
    }
})();
