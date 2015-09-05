package barqsoft.footballscores.service;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Date;

import barqsoft.footballscores.DatabaseContract;
import barqsoft.footballscores.MainActivity;
import barqsoft.footballscores.R;
import barqsoft.footballscores.Utilies;
import barqsoft.footballscores.widget.ScoresWidget;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * helper methods.
 */
public class WidgetUpdateService extends IntentService {
    // What this IntentService can perform
    private static final String ACTION_UPDATED_SCORES = "barqsoft.footballscores.service.action.UPDATED_SCORES";

    // Columns that are going to be retrieved and used in the widget
    private static final String[] WIDGET_COLUMNS = {
            DatabaseContract.scores_table.MATCH_ID,
            DatabaseContract.scores_table.HOME_COL,
            DatabaseContract.scores_table.AWAY_COL,
            DatabaseContract.scores_table.HOME_GOALS_COL,
            DatabaseContract.scores_table.AWAY_GOALS_COL
    };

    // Column indexes that match the projection
    private static final int INDEX_MATCH_ID = 0;
    private static final int INDEX_HOME_COL = 1;
    private static final int INDEX_AWAY_COL = 2;
    private static final int INDEX_HOME_GOALS_COL = 3;
    private static final int INDEX_AWAY_GOALS_COL = 4;

    /**
     * Starts this service to perform the widget update with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionUpdatedScores(Context context) {
        Intent intent = new Intent(context, WidgetUpdateService.class);
        intent.setAction(ACTION_UPDATED_SCORES);
        context.startService(intent);
    }

    public WidgetUpdateService() {
        super("WidgetUpdateService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_UPDATED_SCORES.equals(action)) {
                handleUpdatedScores();
            }
        }
    }

    /**
     * Handle updated Scores in the provided background thread with the provided
     * parameters.
     */
    private void handleUpdatedScores() {
        // Retrieve all of the widget ids: these are the widgets we need to update
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        // Getting today's date as a string
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        String today = df.format(new Date(System.currentTimeMillis()));

        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(this,
                ScoresWidget.class));
        Uri scoresForDateUri = DatabaseContract.scores_table.buildScoreWithDate();
        Cursor data = getContentResolver().query(scoresForDateUri, WIDGET_COLUMNS, null, new String[]{today}, null);

        if (data == null) {
            Log.e("WUT", "THIS IS NOT WORKING");
        }

        if (!data.moveToFirst()) {
            data.close();
            return;
        }

        // extracting the match data from the Cursor
        String homeTeam = data.getString(INDEX_HOME_COL);
        String awayTeam = data.getString(INDEX_AWAY_COL);
        int homeGoals = data.getInt(INDEX_HOME_GOALS_COL);
        int awayGoals = data.getInt(INDEX_AWAY_GOALS_COL);

        data.close();

        // looping through all widgets to update them
        for (int appWidgetId : appWidgetIds) {

            // Construct the RemoteViews object
            RemoteViews views = new RemoteViews(getPackageName(), R.layout.scores_widget);

            views.setTextViewText(R.id.home_name, homeTeam);
            views.setTextViewText(R.id.away_name, awayTeam);
            views.setTextViewText(R.id.score_textview, Utilies.getScores(homeGoals, awayGoals));
            views.setImageViewResource(R.id.home_crest, Utilies.getTeamCrestByTeamName(homeTeam));
            views.setImageViewResource(R.id.away_crest, Utilies.getTeamCrestByTeamName(awayTeam));

            Intent intent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
            views.setOnClickPendingIntent(R.id.score_textview, pendingIntent);

            // Setting the corresponding content descriptions to the ImageViews
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                setRemoteContentDescription(views, R.id.home_crest, homeTeam);
                setRemoteContentDescription(views, R.id.away_crest, awayTeam);
            }

            // Instruct the widget manager to update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    /**
     * Setting the contentDescription for the ImageView in the widget
     * @param views the layout of the widget
     * @param viewId the id of the ImageView that needs a description
     * @param description the corresponding description for the ImageView
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    private void setRemoteContentDescription(RemoteViews views, int viewId, String description) {
        views.setContentDescription(viewId, description);
    }
}
