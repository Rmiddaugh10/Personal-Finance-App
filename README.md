# Personal Finance App

A comprehensive Android finance tracking application built with Kotlin and Jetpack Compose that helps users manage finances, track budgets, and monitor spending patterns. Currently in active development with planned feature expansions.

### Current Core Features:
- CSV bank statement import with automatic transaction categorization
- Custom budget category creation and management
- Real-time budget vs. actual spending analysis
- Paycheck calculator with comprehensive tax handling
- Work schedule tracking with automated wage calculations
- Multi-account balance tracking
- Customizable tax settings and deductions management

### In Development:
- Database optimization for year transitions
- Data persistence improvements
- Category pattern matching refinement
- Enhanced filtering system

### Planned Features:
1. Banking Integration:
- Direct bank API connections
- Real-time transaction updates
- Automated account balance syncing

2. Enhanced Analytics:
- Spending pattern analysis
- Trend visualization
- Predictive budget recommendations
- Investment tracking
- Custom report generation

3. Smart Features:
- Push notifications for:
  - Budget thresholds
  - Bill reminders
  - Unusual spending patterns
  - Upcoming paycheck estimates
- Bill payment tracking
- Savings goals
- Debt management tools

4. System Improvements:
- Performance optimization for large datasets
- Enhanced backup and restore functionality
- Cross-device sync capabilities
- Export functionality for tax preparation

### Technical Roadmap:
- Implementation of WorkManager for background processing
- Enhanced error handling and recovery
- UI/UX refinements
- Expanded test coverage
- Security enhancements

### Known Issues:
- Year transition data display bugs
- Filter persistence challenges
- Category update propagation delays

Open to community contributions and feature suggestions. Project follows MVVM architecture with Clean Architecture principles for maintainability and scalability.

## Screenshots & Features

### Home Screen Views
<img src="https://github.com/user-attachments/assets/583866a9-1bbf-435d-a857-47e52be007b9" width="300">

Main dashboard with account overview and quick access to features.

<img src="https://github.com/user-attachments/assets/2261d26a-56d9-4c7e-966b-99134f8dba89" width="300">

Extended view showing quick budget overview section.

<img src="https://github.com/user-attachments/assets/ac9aa3a9-af11-46fc-9103-91f2fb7633dd" width="300">

Full budget overview section with detailed category tracking.

### Budget Management
<img src="https://github.com/user-attachments/assets/dec95053-65c2-48be-b195-32aa60db5977" width="300">

Monthly budget configuration screen for setting category limits and tracking spending.

### Budget Analysis
<img src="https://github.com/user-attachments/assets/59fe9d3d-dbba-4ff5-84a8-83b3b002a067" width="300">

Budget overview with filtering options by categories, years, and months.

<img src="https://github.com/user-attachments/assets/9940cf8f-d830-4238-a5c1-ff7769654c1a" width="300">

Clean view of budget performance without filters displayed.

### Wallet Features
<img src="https://github.com/user-attachments/assets/02a28ee0-f40c-43a2-8972-e9b06be1cb4f" width="300">

Cash balance tracking with update history.

<img src="https://github.com/user-attachments/assets/d7b1e552-cde7-433d-8afd-5a74c1fea4d5" width="300">

Upcoming paycheck estimates and payment schedule.

### Work Schedule Management
<img src="https://github.com/user-attachments/assets/82e18462-fa9b-45ec-b293-4b02b7b25fb0" width="300">

Work schedule input interface for tracking hours and shifts.

### Settings and Configuration
<img src="https://github.com/user-attachments/assets/86447648-03d7-4852-914a-9c34e05aad41" width="300">

Employment settings including salary/hourly selection, tax configurations, and deductions management.

### Transaction Management
<img src="https://github.com/user-attachments/assets/88752c27-b841-47a7-ab2b-5b733ba37757" width="300">

Recent transactions view showing last 50 entries.

### CSV Import Features
<img src="https://github.com/user-attachments/assets/fe19e1e3-f34a-4714-8703-57381d01c373" width="300">

CSV import interface for bank statements.

<img src="https://github.com/user-attachments/assets/7c69adcb-ae45-4949-ac6c-8ee5d713431d" width="300">

Manual transaction entry interface.

<img src="https://github.com/user-attachments/assets/ab6ae454-4b74-49aa-95f4-e457df19511e" width="300">

Data management with confirmation dialogs for important actions.

### Category Management
<img src="https://github.com/user-attachments/assets/5d66aa53-e753-4b1f-b2a6-1b0b609f6bae" width="300">

Category selection interface for quick budget overview customization.

## Technical Features
- Built with Kotlin and Jetpack Compose
- Room database for local data persistence
- MVVM architecture pattern
- Coroutines for asynchronous operations
- Clean Architecture principles
- Material Design 3 components
- Custom animations and transitions
- Comprehensive test coverage
