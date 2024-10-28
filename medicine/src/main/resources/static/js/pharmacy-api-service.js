// API Service for handling all backend requests
const API_BASE_URL = '/api';

class PharmacyAPIService {
    // Medicine endpoints
    static async getAllMedicines() {
        try {
            const response = await fetch(`${API_BASE_URL}/medicines`);
            return await response.json();
        } catch (error) {
            console.error('Error fetching medicines:', error);
            throw error;
        }
    }

    static async addMedicine(medicineData) {
        try {
            const response = await fetch(`${API_BASE_URL}/medicines`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(medicineData)
            });
            return await response.json();
        } catch (error) {
            console.error('Error adding medicine:', error);
            throw error;
        }
    }

    static async updateMedicine(id, medicineData) {
        try {
            const response = await fetch(`${API_BASE_URL}/medicines/${id}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(medicineData)
            });
            return await response.json();
        } catch (error) {
            console.error('Error updating medicine:', error);
            throw error;
        }
    }

    static async deleteMedicine(id) {
        try {
            await fetch(`${API_BASE_URL}/medicines/${id}`, {
                method: 'DELETE'
            });
        } catch (error) {
            console.error('Error deleting medicine:', error);
            throw error;
        }
    }

    static async searchMedicines(name) {
        try {
            const response = await fetch(`${API_BASE_URL}/medicines/search?name=${encodeURIComponent(name)}`);
            return await response.json();
        } catch (error) {
            console.error('Error searching medicines:', error);
            throw error;
        }
    }

    static async getExpiringMedicines(date) {
        try {
            const response = await fetch(`${API_BASE_URL}/medicines/expiring?before=${date}`);
            return await response.json();
        } catch (error) {
            console.error('Error fetching expiring medicines:', error);
            throw error;
        }
    }

    static async getLowStockMedicines(threshold) {
        try {
            const response = await fetch(`${API_BASE_URL}/medicines/low-stock?threshold=${threshold}`);
            return await response.json();
        } catch (error) {
            console.error('Error fetching low stock medicines:', error);
            throw error;
        }
    }

    static async updateStock(id, quantity) {
        try {
            await fetch(`${API_BASE_URL}/medicines/${id}/stock?quantity=${quantity}`, {
                method: 'PUT'
            });
        } catch (error) {
            console.error('Error updating stock:', error);
            throw error;
        }
    }

    // Sales endpoints
    static async createSale(saleData) {
        try {
            const response = await fetch(`${API_BASE_URL}/sales`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(saleData)
            });
            return await response.json();
        } catch (error) {
            console.error('Error creating sale:', error);
            throw error;
        }
    }

    static async getSales(startDate, endDate) {
        try {
            const response = await fetch(
                `${API_BASE_URL}/sales?start=${startDate.toISOString()}&end=${endDate.toISOString()}`
            );
            return await response.json();
        } catch (error) {
            console.error('Error fetching sales:', error);
            throw error;
        }
    }

    static async getCustomerPurchaseHistory(customerId) {
        try {
            const response = await fetch(`${API_BASE_URL}/sales/customer/${customerId}`);
            return await response.json();
        } catch (error) {
            console.error('Error fetching customer purchase history:', error);
            throw error;
        }
    }

    static async getSalesPdfReport(startDate, endDate) {
        try {
            const response = await fetch(
                `${API_BASE_URL}/sales/report/pdf?start=${startDate.toISOString()}&end=${endDate.toISOString()}`,
                {
                    headers: {
                        'Accept': 'application/pdf'
                    }
                }
            );
            return await response.blob();
        } catch (error) {
            console.error('Error generating PDF report:', error);
            throw error;
        }
    }

    // Reports endpoints
    static async getSalesReport(startDate, endDate) {
        try {
            const response = await fetch(
                `${API_BASE_URL}/reports/sales?start=${startDate.toISOString()}&end=${endDate.toISOString()}`
            );
            return await response.json();
        } catch (error) {
            console.error('Error fetching sales report:', error);
            throw error;
        }
    }

    static async getInventoryReport() {
        try {
            const response = await fetch(`${API_BASE_URL}/reports/inventory`);
            return await response.json();
        } catch (error) {
            console.error('Error fetching inventory report:', error);
            throw error;
        }
    }
}