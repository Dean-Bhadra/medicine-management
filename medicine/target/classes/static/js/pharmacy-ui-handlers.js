// Dashboard functionality
async function showDashboard() {
    try {
        // Hide all views except dashboard
        document.querySelectorAll('.main-content > div').forEach(div => div.style.display = 'none');
        document.getElementById('dashboardView').style.display = 'block';

        // Fetch dashboard data
        const [medicines, lowStock, expiring, sales] = await Promise.all([
            PharmacyAPIService.getAllMedicines(),
            PharmacyAPIService.getLowStockMedicines(10),
            PharmacyAPIService.getExpiringMedicines(new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0]),
            PharmacyAPIService.getSales(new Date(Date.now() - 7 * 24 * 60 * 60 * 1000), new Date())
        ]);

        // Update dashboard counters
        document.getElementById('totalMedicines').textContent = medicines.length;
        document.getElementById('lowStockItems').textContent = lowStock.length;
        document.getElementById('expiringSoon').textContent = expiring.length;
        document.getElementById('totalSales').textContent = `$${sales.reduce((sum, sale) => sum + sale.total, 0).toFixed(2)}`;

        // Update low stock alerts
        const alertsHtml = lowStock.map(medicine =>
            `<div class="alert alert-warning">
                ${medicine.name} - Only ${medicine.stock} units left
            </div>`
        ).join('');
        document.getElementById('lowStockAlerts').innerHTML = alertsHtml;
    } catch (error) {
        console.error('Error loading dashboard:', error);
        alert('Error loading dashboard data');
    }
}

// Medicines functionality
async function showMedicines() {
    try {
        document.querySelectorAll('.main-content > div').forEach(div => div.style.display = 'none');
        document.getElementById('medicinesView').style.display = 'block';

        const medicines = await PharmacyAPIService.getAllMedicines();
        const tableBody = document.querySelector('#medicinesView .table-responsive');

        tableBody.innerHTML = medicines.map(medicine => `
            <tr>
                <td>${medicine.name}</td>
                <td>${medicine.stock}</td>
                <td>$${medicine.price.toFixed(2)}</td>
                <td>${new Date(medicine.expiryDate).toLocaleDateString()}</td>
                <td>
                    <button onclick="editMedicine(${medicine.id})" class="btn btn-sm btn-primary">Edit</button>
                    <button onclick="deleteMedicine(${medicine.id})" class="btn btn-sm btn-danger">Delete</button>
                </td>
            </tr>
        `).join('');
    } catch (error) {
        console.error('Error loading medicines:', error);
        alert('Error loading medicines data');
    }
}

// Sales functionality
async function showSales() {
    try {
        document.querySelectorAll('.main-content > div').forEach(div => div.style.display = 'none');
        document.getElementById('salesView').style.display = 'block';

        const endDate = new Date();
        const startDate = new Date(endDate.getTime() - 30 * 24 * 60 * 60 * 1000); // Last 30 days
        const sales = await PharmacyAPIService.getSales(startDate, endDate);

        const tableBody = document.querySelector('#salesView .table-responsive');
        tableBody.innerHTML = sales.map(sale => `
            <tr>
                <td>${new Date(sale.date).toLocaleDateString()}</td>
                <td>${sale.customerName}</td>
                <td>${sale.items.length}</td>
                <td>$${sale.total.toFixed(2)}</td>
                <td>
                    <button onclick="viewSaleDetails(${sale.id})" class="btn btn-sm btn-info">View</button>
                </td>
            </tr>
        `).join('');
    } catch (error) {
        console.error('Error loading sales:', error);
        alert('Error loading sales data');
    }
}

// Modal handlers
async function handleAddMedicine(event) {
    event.preventDefault();
    const form = event.target;
    const formData = new FormData(form);

    try {
        const medicineData = {
            name: formData.get('name'),
            manufacturer: formData.get('manufacturer'),
            price: parseFloat(formData.get('price')),
            stock: parseInt(formData.get('stock')),
            expiryDate: formData.get('expiryDate'),
            batchNumber: formData.get('batchNumber'),
            description: formData.get('description'),
            requiresPrescription: formData.get('requiresPrescription') === 'on'
        };

        await PharmacyAPIService.addMedicine(medicineData);
        bootstrap.Modal.getInstance(document.getElementById('medicineModal')).hide();
        showMedicines();
    } catch (error) {
        console.error('Error adding medicine:', error);
        alert('Error adding medicine');
    }
}

async function handleCreateSale(event) {
    event.preventDefault();
    const form = event.target;
    const formData = new FormData(form);

    try {
        const saleData = {
            customerName: formData.get('customerName'),
            customerPhone: formData.get('customerPhone'),
            prescriptionNumber: formData.get('prescriptionNumber'),
            items: Array.from(document.querySelectorAll('.sale-item')).map(item => ({
                medicineId: item.querySelector('select').value,
                quantity: parseInt(item.querySelector('input[type="number"]').value)
            }))
        };

        await PharmacyAPIService.createSale(saleData);
        bootstrap.Modal.getInstance(document.getElementById('saleModal')).hide();
        showSales();
    } catch (error) {
        console.error('Error creating sale:', error);
        alert('Error creating sale');
    }
}

// Initialize the application
document.addEventListener('DOMContentLoaded', () => {
    showDashboard();

    // Add event listeners for modals
    document.getElementById('medicineModal').addEventListener('submit', handleAddMedicine);
    document.getElementById('saleModal').addEventListener('submit', handleCreateSale);

    // Add event listeners for report generation
    document.querySelector('#reportsView button').addEventListener('click', async () => {
        const startDate = new Date(document.getElementById('reportStartDate').value);
        const endDate = new Date(document.getElementById('reportEndDate').value);

        try {
            const blob = await PharmacyAPIService.getSalesPdfReport(startDate, endDate);
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = 'sales-report.pdf';
            a.click();
            window.URL.revokeObjectURL(url);
        } catch (error) {
            console.error('Error generating report:', error);
            alert('Error generating report');
        }
    });
});