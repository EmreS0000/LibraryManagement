const API_URL = '/api/v1';
const USER_ID = localStorage.getItem('userId') || 1; // Default to 1 if no auth

document.addEventListener('DOMContentLoaded', () => {
    initTabs();
    initLogout();
    loadBooks();
    loadLoans();

    // Check auth
    if (!localStorage.getItem('userEmail')) {
        window.location.href = 'index.html';
    } else {
        document.getElementById('welcome-message').textContent = `User: ${localStorage.getItem('userEmail')}`;
    }
});

function initTabs() {
    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
            document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));

            btn.classList.add('active');
            document.getElementById(btn.dataset.tab).classList.add('active');
        });
    });
}

function initLogout() {
    document.getElementById('logoutBtn').addEventListener('click', () => {
        localStorage.clear();
        window.location.href = 'index.html';
    });
}

function showToast(msg) {
    const toast = document.getElementById('toast');
    toast.textContent = msg;
    toast.classList.remove('hidden');
    toast.style.display = 'block'; // Ensure visibility
    toast.className = 'alert alert-success'; // Reuse alert styles fixed to bottom
    toast.style.position = 'fixed';
    toast.style.bottom = '20px';
    toast.style.right = '20px';
    setTimeout(() => toast.style.display = 'none', 3000);
}

// ------ BOOKS ------
async function loadBooks() {
    try {
        const res = await fetch(`${API_URL}/books?size=50`);
        const data = await res.json();
        const books = data.content || [];

        const container = document.getElementById('booksList');
        container.innerHTML = books.map(book => `
            <div class="book-item" data-isbn="${book.isbn}">
                <div class="book-meta">
                    <div class="book-title">${book.title}</div>
                    <div class="book-author">by ${book.author}</div>
                    <div class="stock-badge ${book.availableQuantity === 0 ? 'out' : ''}">
                        ${book.availableQuantity} in stock
                    </div>
                </div>
                <button 
                    class="borrow-btn" 
                    onclick="borrowBook(${book.id})"
                    ${book.availableQuantity === 0 ? 'disabled' : ''}>
                    ${book.availableQuantity === 0 ? 'Out of Stock' : 'Borrow Book'}
                </button>
            </div>
        `).join('');
    } catch (err) {
        console.error('Failed to load books', err);
    }
}

// Global scope for onclick
window.borrowBook = async (bookId) => {
    try {
        const res = await fetch(`${API_URL}/loans/borrow`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                bookId: bookId,
                memberId: parseInt(USER_ID)
            })
        });

        if (res.ok) {
            showToast('Book borrowed successfully!');
            loadBooks(); // Refresh stock
            loadLoans(); // Refresh loans
        } else {
            const err = await res.json();
            alert('Error: ' + err.message);
        }
    } catch (e) {
        alert('Failed to borrow book');
    }
};

// ------ LOANS ------
async function loadLoans() {
    try {
        const res = await fetch(`${API_URL}/loans/my-loans/${USER_ID}`);
        // Note: Backend endpoint might need adjustment to match 'my-loans/{id}' or filter by user
        // Assuming we created getLoansByMember in Controller or Service
        // If not, we might need to filter client side or add endpoint. 
        // Re-using getAllLoans for simplicity if strict filtering not enforced in this demo scope
        // Actually earlier 'getLoansByMember' was added to Service but verify Controller exposed it.
        // If Controller didn't expose /my-loans, we might hit /loans
        // Let's assume standard REST: GET /loans/member/{id} or similar.
        // Checking Controller... LoanController usually has logic.
        // Falling back to "GET /loans" if "my-loans" missing, filtering client side if needed.

        // Let's try GET /loans?memberId={id} if implemented, or just GET /loans and filter
        // Actually, looking at LoanController, we might not have exposed member specific endpoint.
        // I will use fetch(`${API_URL}/loans`) and filter in JS for this "Front End Task".

        const allRes = await fetch(`${API_URL}/loans`);
        const allData = await allRes.json();
        const loans = (allData.content || allData).filter(l => l.memberId == USER_ID);

        const tbody = document.getElementById('loansList');
        tbody.innerHTML = loans.map(loan => `
            <tr data-status="${loan.status}">
                <td>Book ID ${loan.bookId}</td> <!-- Ideally would fetch book title -->
                <td>${new Date(loan.loanDate).toLocaleDateString()}</td>
                <td>${loan.returnDate ? new Date(loan.returnDate).toLocaleDateString() : '-'}</td>
                <td><span class="status-badge status-${loan.status.toLowerCase()}">${loan.status}</span></td>
                <td>
                    ${loan.status === 'ACTIVE' ?
                `<button class="btn-outline return-btn" onclick="returnBook(${loan.id})">Return</button>` :
                '-'}
                </td>
            </tr>
        `).join('');
    } catch (err) {
        console.error('Failed to load loans', err);
    }
}

window.returnBook = async (loanId) => {
    if (!confirm('Return this book?')) return;
    try {
        const res = await fetch(`${API_URL}/loans/return/${loanId}`, { method: 'POST' });
        if (res.ok) {
            showToast('Book returned successfully');
            loadBooks();
            loadLoans();
        } else {
            alert('Failed to return book');
        }
    } catch (e) {
        alert('System error');
    }
};
