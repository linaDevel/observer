function setUpdates(data) {
    if (data.view == "report") {
        if (data.type == "comment") {
            var comment_value = $('#comment_data_' + data.id).first();
            var comment = $('#comment_' + data.id).first();

            var state_group = $('#state_group_' + data.group).first();
            var state = $('#state_' + data.id).first();

            if (data.success) {
                if (data.ok) {
                    state.addClass("text-success");
                    state.removeClass("text-danger");

                    state.text("done_all");
                } else {
                    state.addClass("text-danger");
                    state.removeClass("text-success");

                    state.text("error");
                }

                if (data.group_ok) {
                    state_group.addClass("text-success");
                    state_group.removeClass("text-danger");

                    state_group.text("done_all");
                } else {
                    state_group.addClass("text-danger");
                    state_group.removeClass("text-success");

                    state_group.text("error");
                }

                comment.text(comment_value.val());
            }
        }
    }
}

function setComment(reportId) {
    var comment = $('#comment_data_' + reportId).first();

    send({
        action: "comment",
        view: "report",
        id: reportId,
        comment: comment.val()
    })
}