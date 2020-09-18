$(function() {
    //Accessibility
    var errorSummary =  $('#error-summary-display'),
    $input = $('input:text')
    //Error summary focus
    if (errorSummary){ errorSummary.focus() }

    //Trim inputs and Capitalize postode
    $('[type="submit"]').click(function(){
        $input.each( function(){
            if($(this).val() && $(this).attr('name') === 'postcode'){
                $(this).val($(this).val().toUpperCase().replace(/\s\s+/g, ' ').trim())
            }else{
                $(this).val($(this).val().trim())
            }
        });
    });

    // ------------------------------------
    // Introduce direct skip link control, to work around voiceover failing of hash links
    // https://bugs.webkit.org/show_bug.cgi?id=179011
    // https://axesslab.com/skip-links/
    // ------------------------------------
    $('.skiplink').click(function(e) {
        e.preventDefault();
        $(':header:first').attr('tabindex', '-1').focus();
    });

    //Add aria-hidden to hidden inputs
    $('[type="hidden"]').attr("aria-hidden", true);

    $('a[role=button]').keyup(function(e) {
        // get the target element
        var target = e.target;

        // if the element has a role=’button’ and the pressed key is a space, we’ll simulate a click
        if (e.keyCode === 32) {
            e.preventDefault();
            // trigger the target’s click event
            target.click()
        }
    });

    var showHideContent = new GOVUK.ShowHideContent()
    showHideContent.init()
});
