package pt.isel.services.users;

public interface TicketService {
    String createTicket(Long userId);

    Long validateAndConsumeTicket(String ticket);
}