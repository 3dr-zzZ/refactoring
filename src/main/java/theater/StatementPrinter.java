package theater;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;

/**
 * This class generates a statement for a given invoice of performances.
 */
public class StatementPrinter {
    private static final String TYPE_COMEDY = "comedy";
    private static final String TYPE_TRAGEDY = "tragedy";

    private final Invoice invoice;
    private final Map<String, Play> plays;

    private int lastTotalAmount;
    private int lastTotalVolumeCredits;

    public StatementPrinter(Invoice invoice, Map<String, Play> plays) {
        this.invoice = invoice;
        this.plays = plays;
    }

    public Invoice getInvoice() {
        return invoice;
    }

    public Map<String, Play> getPlays() {
        return plays;
    }

    /**
     * Returns the Play associated with the given play ID.
     *
     * @param playId the ID of the play to retrieve
     * @return the Play object matching the given ID
     * @null nothing
     */
    public Play getPlay(String playId) {
        return plays.get(playId);
    }

    /**
     * Computes the amount owed for a given performance and play.
     *
     * @param perf the performance instance
     * @param play the play performed
     * @return the amount in cents
     * @null nothing
     */
    public int getAmount(Performance perf, Play play) {
        return calculateAmount(perf, play);
    }

    /**
     * Calculates the volume credits earned for a given performance.
     *
     * @param perf the performance instance
     * @param play the associated play
     * @return the number of volume credits earned
     * @null nothing
     */
    public int getVolumeCredits(Performance perf, Play play) {
        int credits = Math.max(perf.getAudience() - Constants.BASE_VOLUME_CREDIT_THRESHOLD, 0);
        if (TYPE_COMEDY.equals(play.getType())) {
            credits += perf.getAudience() / Constants.COMEDY_EXTRA_VOLUME_FACTOR;
        }
        return credits;
    }

    /**
     * Formats a numeric amount (in cents) into a USD currency string.
     *
     * @param amount the amount in cents
     * @return formatted USD currency string
     * @null nothing
     */
    public String usd(int amount) {
        final NumberFormat frmt = NumberFormat.getCurrencyInstance(Locale.US);
        return frmt.format(amount / (double) Constants.PERCENT_FACTOR);
    }

    private int calculateAmount(Performance perf, Play play) {
        int thisAmount = 0;
        switch (play.getType()) {
            case TYPE_TRAGEDY:
                thisAmount = Constants.TRAGEDY_BASE_AMOUNT;
                if (perf.getAudience() > Constants.TRAGEDY_AUDIENCE_THRESHOLD) {
                    thisAmount += Constants.TRAGEDY_OVER_BASE_CAPACITY_PER_PERSON
                            * (perf.getAudience() - Constants.TRAGEDY_AUDIENCE_THRESHOLD);
                }
                break;
            case TYPE_COMEDY:
                thisAmount = Constants.COMEDY_BASE_AMOUNT;
                if (perf.getAudience() > Constants.COMEDY_AUDIENCE_THRESHOLD) {
                    thisAmount += Constants.COMEDY_OVER_BASE_CAPACITY_AMOUNT
                            + (Constants.COMEDY_OVER_BASE_CAPACITY_PER_PERSON
                            * (perf.getAudience() - Constants.COMEDY_AUDIENCE_THRESHOLD));
                }
                thisAmount += Constants.COMEDY_AMOUNT_PER_AUDIENCE * perf.getAudience();
                break;
            default:
                throw new RuntimeException(String.format("unknown type: %s", play.getType()));
        }
        return thisAmount;
    }

    private String formatLine(Play play, int thisAmount, Performance perf, NumberFormat frmt) {
        return String.format(
                "  %s: %s (%s seats)%n",
                play.getName(),
                frmt.format(thisAmount / Constants.PERCENT_FACTOR),
                perf.getAudience()
        );
    }

    private String formatTotals(int totalAmount, int volumeCredits, NumberFormat frmt) {
        return String.format("Amount owed is %s%n", frmt.format(totalAmount / Constants.PERCENT_FACTOR))
                + String.format("You earned %s credits%n", volumeCredits);
    }

    /**
     * Returns a formatted statement of the invoice associated with this printer.
     * @return the formatted statement
     * @throws RuntimeException if one of the play types is not known
     */
    public String statement() {
        int totalAmount = 0;
        int volumeCredits = 0;
        final StringBuilder result = new StringBuilder(
                "Statement for "
                        + invoice.getCustomer()
                        + System.lineSeparator()
        );

        final NumberFormat frmt = NumberFormat.getCurrencyInstance(Locale.US);

        for (Performance p : invoice.getPerformances()) {
            final Play play = plays.get(p.getPlayID());

            final int thisAmount = calculateAmount(p, play);

            // add volume credits
            volumeCredits += Math.max(p.getAudience() - Constants.BASE_VOLUME_CREDIT_THRESHOLD, 0);
            // add extra credit for every five comedy attendees
            if (TYPE_COMEDY.equals(play.getType())) {
                volumeCredits += p.getAudience() / Constants.COMEDY_EXTRA_VOLUME_FACTOR;
            }

            // print line for this order
            result.append(formatLine(play, thisAmount, p, frmt));
            totalAmount += thisAmount;
        }
        // set tracking fields before returning
        lastTotalAmount = totalAmount;
        lastTotalVolumeCredits = volumeCredits;
        result.append(formatTotals(totalAmount, volumeCredits, frmt));
        return result.toString();
    }

    /**
     * Returns the most recently computed total amount from the last statement.
     *
     * @return the total amount in cents
     * @null nothing
     */
    public int getTotalAmount() {
        return lastTotalAmount;
    }

    /**
     * Returns the most recently computed total volume credits.
     *
     * @return total volume credits
     * @null nothing
     */
    public int getTotalVolumeCredits() {
        return lastTotalVolumeCredits;
    }
}
