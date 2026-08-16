// Kleiner Funktionstest: zeigt, dass JavaScript und localStorage laufen.
// Diese Datei darfst du loeschen, sobald du deine eigene App hochlaedst.

const button = document.getElementById('counter');
let taps = Number(localStorage.getItem('taps') || 0);

function render() {
    button.textContent = taps === 1 ? '1-mal getippt' : `${taps}-mal getippt`;
}

button.addEventListener('click', () => {
    taps += 1;
    localStorage.setItem('taps', String(taps));
    render();
});

render();
