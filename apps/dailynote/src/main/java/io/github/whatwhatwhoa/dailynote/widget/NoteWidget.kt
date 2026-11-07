package io.github.whatwhatwhoa.dailynote.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import io.github.whatwhatwhoa.dailynote.Jump
import io.github.whatwhatwhoa.dailynote.MainActivity
import io.github.whatwhatwhoa.dailynote.PaperArt
import io.github.whatwhatwhoa.dailynote.R

/**
 * The note on the home screen, at any size up to a whole page. It shows the page and scrolls;
 * a tap on a line opens the app with the cursor on that line. Writing and erasing happen in the
 * app: a home screen can't host a text field or tell a swipe on a widget from a page turn.
 */
class NoteWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        for (id in ids) update(context, manager, id)
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) {
        update(context, manager, id)
    }

    companion object {
        /** Redraws every placed widget: after the app leaves the screen, as the note or paper changed. */
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            for (id in manager.getAppWidgetIds(ComponentName(context, NoteWidget::class.java))) {
                update(context, manager, id)
            }
        }

        // The lines go to the widget with each update (no list service): newer Android fetches a
        // service's list once and keeps it, so a widget fed that way never showed later edits.
        private fun update(context: Context, manager: AppWidgetManager, id: Int) {
            val page = WidgetPage(context, manager, id)
            val views = RemoteViews(context.packageName, R.layout.widget_note)
            views.setInt(R.id.paper, "setColorFilter", PaperArt.paperColor(page.paper))
            views.setImageViewBitmap(R.id.band, page.band())

            val open = Intent(context, MainActivity::class.java)
            views.setOnClickPendingIntent(
                R.id.band, PendingIntent.getActivity(context, 0, open, PendingIntent.FLAG_IMMUTABLE),
            )
            // Each line fills in where the cursor goes, so the template is mutable.
            views.setPendingIntentTemplate(
                R.id.lines,
                PendingIntent.getActivity(
                    context, 1, Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                ),
            )
            val lines = RemoteViews.RemoteCollectionItems.Builder().setViewTypeCount(1)
            for (i in 0 until page.rows) lines.addItem(i.toLong(), row(context, page, i))
            views.setRemoteAdapter(R.id.lines, lines.build())
            manager.updateAppWidget(id, views)
        }

        /**
         * Line [i]: the paper strip (the same bitmap for every line), the ink, where a tap goes.
         * The launcher lays each row over a recycled one (a blank line's, say), keeping whatever
         * the row doesn't set, so every row sets all it shows: the ink's visibility too.
         */
        private fun row(context: Context, page: WidgetPage, i: Int): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_row)
            views.setImageViewBitmap(R.id.strip, page.strip(i))
            val ink = page.ink(i)
            if (ink != null) {
                views.setViewVisibility(R.id.ink, View.VISIBLE)
                views.setImageViewBitmap(R.id.ink, ink.first)
                views.setViewPadding(R.id.ink, page.textStart + ink.second, 0, 0, 0)
            } else {
                views.setViewVisibility(R.id.ink, View.GONE)
            }
            views.setContentDescription(
                R.id.row, page.line(i).trim().ifEmpty { context.getString(R.string.widget_blank_line) },
            )
            val (offset, newlines) = page.jumpFor(i)
            views.setOnClickFillInIntent(R.id.row, Intent().putExtra(Jump.OFFSET, offset).putExtra(Jump.NEWLINES, newlines))
            return views
        }
    }
}
