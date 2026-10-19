package tr.kitaptakip.dtos;

import java.util.List;

public record DashboardResponse(
        long totalBookCount,
        long activeBookCount,
        long totalCopyCount,
        long availableCopyCount,
        long onLoanCopyCount,
        long activeLoanCount,
        long overdueLoanCount,
        List<LoanResponse> recentLoans,
        List<LoanResponse> overdueLoans
) { }
